/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.undo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLObjectEvent;
import ca.sqlpower.architect.SQLObjectListener;
import ca.sqlpower.architect.swingui.PlayPen;

public class UndoManager extends javax.swing.undo.UndoManager {

    private static final Logger logger = Logger.getLogger(UndoManager.class);

    /**
     * Converts received SQLObjectEvents into UndoableEdits, PropertyChangeEvents
     * into specific edits and adds them to an UndoManager.
     */
    public class SQLObjectUndoableEventAdapter implements UndoCompoundEventListener, SQLObjectListener,
    PropertyChangeListener {

        private final class CompEdit extends CompoundEdit {

            String toolTip;

            public CompEdit(String toolTip) {
                super();
                this.toolTip = toolTip;
            }

            @Override
            public String getPresentationName() {
                return toolTip;
            }

            @Override
            public String getUndoPresentationName() {
                return "Undo " + getPresentationName();
            }

            @Override
            public String getRedoPresentationName() {
                return "Redo " + getPresentationName();
            }

            @Override
            public boolean canUndo() {

                return super.canUndo() && edits.size() > 0;
            }

            @Override
            public boolean canRedo() {
                return super.canRedo() && edits.size() > 0;
            }

            @Override
            public String toString() {
                StringBuffer sb = new StringBuffer();
                for (Object o : edits) {
                    sb.append(o).append("\n");
                }

                return sb.toString();
            }
        }

        private CompoundEdit ce;

        private int compoundEditStackCount;

        public SQLObjectUndoableEventAdapter() {

            ce = null;
            compoundEditStackCount = 0;
        }

        /**
         * You should not undo when in a compound edit
         * 
         * @return
         */
        public boolean canUndoOrRedo() {
            if (ce == null) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Begins a compound edit. Compound edits can be nested, so every call
         * to this method has to be balanced with a call to
         * {@link #compoundGroupEnd()}.
         * 
         * fires a state changed event when a new compound edit is created
         */
        private void compoundGroupStart(String toolTip) {
            if (UndoManager.this.isUndoOrRedoing())
                return;
            compoundEditStackCount++;
            if (compoundEditStackCount == 1) {
                ce = new CompEdit(toolTip);
                fireStateChanged();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("compoundGroupStart: edit stack =" + compoundEditStackCount);
            }
        }

        /**
         * Ends a compound edit. Compound edits can be nested, so every call to
         * this method has to be preceeded by a call to
         * {@link #compoundGroupStart()}.
         * 
         * @throws IllegalStateException
         *             if there wasn't already a compound edit in progress.
         */
        private void compoundGroupEnd() {
            if (UndoManager.this.isUndoOrRedoing())
                return;
            if (compoundEditStackCount <= 0) {
                throw new IllegalStateException("No compound edit in progress");
            }
            compoundEditStackCount--;
            if (compoundEditStackCount == 0)
                returnToEditState();
            if (logger.isDebugEnabled()) {
                logger.debug("compoundGroupEnd: edit stack =" + compoundEditStackCount + " ce=" + ce);
            }
        }

        private void addEdit(UndoableEdit undoEdit) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding new edit: " + undoEdit);
            }

            // if we are not in a compound edit
            if (compoundEditStackCount == 0) {
                UndoManager.this.addEdit(undoEdit);
            } else {
                ce.addEdit(undoEdit);
            }
        }

        public void dbChildrenInserted(SQLObjectEvent e) {
            if (UndoManager.this.isUndoOrRedoing())
                return;

            SQLObjectInsertChildren undoEvent = new SQLObjectInsertChildren();
            undoEvent.createEditFromEvent(e);
            addEdit(undoEvent);

            try {
                ArchitectUtils.listenToHierarchy(this, e.getChildren());
                ArchitectUtils.addUndoListenerToHierarchy(this, e.getChildren());
            } catch (ArchitectException ex) {
                logger.error("SQLObjectUndoableEventAdapter cannot attach to new children", ex);
            }

        }

        public void dbChildrenRemoved(SQLObjectEvent e) {
            if (UndoManager.this.isUndoOrRedoing())
                return;

            SQLObjectRemoveChildren undoEvent = new SQLObjectRemoveChildren();
            undoEvent.createEditFromEvent(e);
            addEdit(undoEvent);
        }

        public void dbObjectChanged(SQLObjectEvent e) {
            if (UndoManager.this.isUndoOrRedoing())
                return;
            if (e.getSource() instanceof SQLDatabase && e.getPropertyName().equals("shortDisplayName")) {
                // this is not undoable at this time.
            } else {
                ArchitectPropertyChangeUndoableEdit undoEvent = new ArchitectPropertyChangeUndoableEdit(e);
                addEdit(undoEvent);
            }
        }

        public void dbStructureChanged(SQLObjectEvent e) {
            logger.error("Unexpected structure change event");

            // too many changes clear undo
            UndoManager.this.discardAllEdits();
        }

        /**
         * Packs property change event into PropertyChangeEdit and then adds
         * to the undo manager.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            if (UndoManager.this.isUndoOrRedoing()) {
                return;
            }
            PropertyChangeEdit edit = new PropertyChangeEdit(evt);
            addEdit(edit);
        }

        /**
         * Return to a single edit state from a compound edit state
         */
        private void returnToEditState() {
            if (compoundEditStackCount != 0) {
                throw new IllegalStateException("The compound edit stack (" + compoundEditStackCount + ") should be 0");
            }
            if (ce != null) {
                ce.end();
                if (ce.canUndo()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Adding compound edit " + ce + " to undo manager");
                    UndoManager.this.addEdit(ce);
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug("Compound edit " + ce + " is not undoable so we are not adding it");
                }
                ce = null;
            }
            fireStateChanged();
            logger.debug("Returning to regular state");
        }

        public void compoundEditStart(UndoCompoundEvent e) {
            if (logger.isDebugEnabled()) {
                logger.debug("compoundEditStart with event: " + e.toString());
            }
            compoundGroupStart(e.getMessage());
        }

        public void compoundEditEnd(UndoCompoundEvent e) {
            if (logger.isDebugEnabled()) {
                logger.debug("compoundEditEnd with event: " + e.toString());
            }
            compoundGroupEnd();
        }
    }

    private SQLObjectUndoableEventAdapter eventAdapter = new SQLObjectUndoableEventAdapter();

    private boolean undoing;

    private boolean redoing;

    private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

    /**
     * Creates a new UndoManager and attaches it to the given PlayPen's
     * component and SQL Object model events.
     * 
     * @param playPen
     *            The play pen to track undo/redo history for.
     * @throws ArchitectException
     *             If the manager fails to listen to all objects in the play
     *             pen's database hierarchy.
     */
    public UndoManager(PlayPen playPen) throws ArchitectException {
        init(playPen, playPen.getSession().getTargetDatabase());
    }

    public UndoManager(SQLObject sqlObjectRoot) throws ArchitectException {
        init(null, sqlObjectRoot);
    }

    private final void init(PlayPen playPen, SQLObject sqlObjectRoot) throws ArchitectException {
        ArchitectUtils.listenToHierarchy(eventAdapter, sqlObjectRoot);
        ArchitectUtils.addUndoListenerToHierarchy(eventAdapter, sqlObjectRoot);
        if (playPen != null) {
            playPen.addUndoEventListener(eventAdapter);
        }
    }

    /**
     * Adds then given edit to this undo manager.
     * 
     * <p>
     * Warning: Edits added here do not respect compounding. You can add a whole
     * CompoundEdit here, but if the current state of the undo manager is that
     * it's in a compound edit, it doesn't matter. You will get individual edits
     * when you add individual edits.
     */
    public synchronized boolean addEdit(UndoableEdit anEdit) {

        if (!(isUndoing() || isRedoing())) {
            if (logger.isDebugEnabled())
                logger.debug("Added new undoableEdit to undo manager " + anEdit);
            boolean success = super.addEdit(anEdit);
            fireStateChanged();
            return success;
        }
        // processing an edit so we pretend to absorb this edit
        return true;
    }

    /**
     * Calls super.undo() then refreshes the undo/redo actions.
     */
    @Override
    public synchronized void undo() throws CannotUndoException {
        undoing = true;
        super.undo();
        fireStateChanged();
        undoing = false;
    }

    /**
     * Calls super.redo() then refreshes the undo/redo actions.
     */
    @Override
    public synchronized void redo() throws CannotRedoException {
        redoing = true;
        super.redo();
        fireStateChanged();
        redoing = false;
    }

    @Override
    public synchronized boolean canUndo() {
        return super.canUndo() && eventAdapter.canUndoOrRedo();
    }

    @Override
    public synchronized boolean canRedo() {
        return super.canRedo() && eventAdapter.canUndoOrRedo();
    }

    /* Public getters and setters appear after this point */

    public int getUndoableEditCount() {
        if (editToBeUndone() == null)
            return 0;
        int count;
        // edits is a 0 based vector
        count = this.edits.indexOf(this.editToBeUndone()) + 1;
        return count;
    }

    public int getRedoableEditCount() {
        if (editToBeRedone() == null)
            return 0;
        int count;
        count = edits.size() - this.edits.indexOf(this.editToBeRedone());
        return count;

    }

    public boolean isRedoing() {
        return redoing;
    }

    public boolean isUndoing() {
        return undoing;
    }

    public boolean isUndoOrRedoing() {
        return undoing || redoing;
    }

    public SQLObjectUndoableEventAdapter getEventAdapter() {
        return eventAdapter;
    }

    // Change event support

    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    /**
     * Notifies listeners that the undo/redo list might have changed.
     */
    public void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener l : changeListeners) {
            l.stateChanged(event);
        }
    }

    public String printUndoVector() {
        StringBuffer sb = new StringBuffer();
        for (Object o : edits) {
            sb.append(o).append("\n");
        }

        return sb.toString();
    }
}