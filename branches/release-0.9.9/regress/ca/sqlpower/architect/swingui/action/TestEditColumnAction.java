/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.swingui.action;

import java.awt.Point;
import java.sql.Types;

import junit.framework.TestCase;
import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.PlayPen;
import ca.sqlpower.architect.swingui.Relationship;
import ca.sqlpower.architect.swingui.TestingArchitectSwingSessionContext;
import ca.sqlpower.architect.swingui.TablePane;
import ca.sqlpower.architect.swingui.event.SelectionEvent;

public class TestEditColumnAction extends TestCase {

	private EditColumnAction editColumn;
	private PlayPen pp;
	private TablePane tp;
	private Relationship r;
	private TablePane tp2;
	
	protected void setUp() throws Exception {
		super.setUp();
        TestingArchitectSwingSessionContext context = new TestingArchitectSwingSessionContext();
        ArchitectSwingSession session = context.createSession();
		editColumn = new EditColumnAction(session);
		pp = new PlayPen(session);
		tp = new TablePane(new SQLTable(session.getTargetDatabase(),true),pp);
		tp.getModel().setName("Table1");
		tp.getModel().addColumn(new SQLColumn(tp.getModel(),"col1",Types.INTEGER,1,1));
		tp.getModel().addColumn(new SQLColumn(tp.getModel(),"col2",Types.INTEGER,1,1));
		
		tp2 = new TablePane(new SQLTable(session.getTargetDatabase(),true),pp);
		tp2.getModel().setName("Table2");
		tp2.getModel().addColumn(new SQLColumn(tp.getModel(),"col1",Types.INTEGER,1,1));
		tp2.getModel().addColumn(new SQLColumn(tp.getModel(),"col2",Types.INTEGER,1,1));
		
		SQLRelationship sqlRelationship = new SQLRelationship();
		pp.addTablePane(tp,new Point());
		pp.addTablePane(tp2, new Point());
		sqlRelationship.attachRelationship(tp.getModel(),tp.getModel(),false);
		r = new Relationship(pp,sqlRelationship);
		pp.addRelationship(r);
		
	}

	public void testTableSelected() throws ArchitectException{
		assertFalse("Action enabled with no items",editColumn.isEnabled());
		tp.setSelected(true,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("Action should be not enabled", editColumn.isEnabled());		
	}
	
	public void testRelationshipSelected() {
		assertFalse("Action enabled with no items",editColumn.isEnabled());
		r.setSelected(true,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(r, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("Action should be disabled", editColumn.isEnabled());		
	}
	
	public void testTableAndRelationshipSelected() {
		assertFalse("Action enabled with no items",editColumn.isEnabled());
		r.setSelected(true,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(r, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		tp.setSelected(true,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("Action should be disabled", editColumn.isEnabled());		
		r.setSelected(false,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(r, SelectionEvent.DESELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("Action should still be disabled", editColumn.isEnabled());
		tp.setSelected(false,SelectionEvent.SINGLE_SELECT);		
	}
	
	public void testColumnSelected() throws ArchitectException{
		assertFalse("Action enabled with no items",editColumn.isEnabled());
		tp.setSelected(true,SelectionEvent.SINGLE_SELECT);
		tp.selectColumn(0);
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertEquals("Editting col1", editColumn.getValue(EditColumnAction.SHORT_DESCRIPTION));		
		tp.selectColumn(1);
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertTrue("Action not enabled", editColumn.isEnabled());		
		tp.selectNone();
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.DESELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("No column is selected, only tables, should disable",editColumn.isEnabled());		
	}
	
	public void testTableAndColumnSelected(){
		assertFalse("Action enabled with no items",editColumn.isEnabled());
		tp.setSelected(true,SelectionEvent.SINGLE_SELECT);
		tp.selectColumn(0);
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		tp2.setSelected(true,SelectionEvent.SINGLE_SELECT);				
		editColumn.itemSelected(new SelectionEvent(tp2, SelectionEvent.SELECTION_EVENT, SelectionEvent.SINGLE_SELECT));		
		assertFalse("Action not enabled", editColumn.isEnabled());
		tp2.setSelected(false,SelectionEvent.SINGLE_SELECT);
		editColumn.itemSelected(new SelectionEvent(tp2, SelectionEvent.DESELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertTrue("Action not enabled", editColumn.isEnabled());		
		tp.selectNone();
		editColumn.itemSelected(new SelectionEvent(tp, SelectionEvent.DESELECTION_EVENT, SelectionEvent.SINGLE_SELECT));
		assertFalse("Only table is selected, should disable",editColumn.isEnabled());
	}

}
