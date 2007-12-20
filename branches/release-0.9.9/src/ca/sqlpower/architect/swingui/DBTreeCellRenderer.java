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
/*
 * Created on Nov 28, 2006
 *
 * This code belongs to SQL Power Group Inc.
 */
package ca.sqlpower.architect.swingui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import ca.sqlpower.architect.ArchitectSession;
import ca.sqlpower.architect.SQLCatalog;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLSchema;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.SQLIndex.Column;
import ca.sqlpower.swingui.SPSUtils;

/**
 * The DBTreeCellRenderer renders nodes of a JTree which are of
 * type SQLObject.  This class is much older than November 2006; it
 * was pulled out of the DBTree.java compilation unit into its own
 * file on this date so it could be used more naturally as the cell
 * renderer for a different JTree.
 *
 * @author fuerth
 * @version $Id$
 */
public class DBTreeCellRenderer extends DefaultTreeCellRenderer {
	public static final ImageIcon dbIcon = SPSUtils.createIcon("Database", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon dbProfiledIcon = SPSUtils.createIcon("Database_profiled", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon targetIcon = SPSUtils.createIcon("Database_target", "SQL Database", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon cataIcon = SPSUtils.createIcon("Catalog", "SQL Catalog", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon schemaIcon = SPSUtils.createIcon("Schema", "SQL Schema", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon tableIcon = SPSUtils.createIcon("Table", "SQL Table", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon tableProfiledIcon = SPSUtils.createIcon("Table_profiled", "SQL Table", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon exportedKeyIcon = SPSUtils.createIcon("ExportedKey", "Exported key", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon importedKeyIcon = SPSUtils.createIcon("ImportedKey", "Imported key", ArchitectSwingSessionContext.ICON_SIZE);
	public static final ImageIcon ownerIcon = SPSUtils.createIcon("Owner", "Owner", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon indexIcon = SPSUtils.createIcon("Index", "Index", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon pkIndexIcon = SPSUtils.createIcon("Index_key", "Primary Key Index", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon uniqueIndexIcon = SPSUtils.createIcon("Index_unique", "Unique Index", ArchitectSwingSessionContext.ICON_SIZE);
    public static final ImageIcon columnIcon = SPSUtils.createIcon("Column", "Column", ArchitectSwingSessionContext.ICON_SIZE);
    private final ArchitectSession session;
   
    
	public DBTreeCellRenderer(ArchitectSession session) {
        super();
        this.session = session;
    }


    public Component getTreeCellRendererComponent(JTree tree,
												  Object value,
												  boolean sel,
												  boolean expanded,
												  boolean leaf,
												  int row,
												  boolean hasFocus) {
		setText(value.toString());
		if (value instanceof SQLDatabase) {
			SQLDatabase db = (SQLDatabase) value;
			if (db.isPlayPenDatabase()) {
				setIcon(targetIcon);
                setText("Project");
			} else {
				setIcon(dbIcon);
			}
		} else if (value instanceof SQLCatalog) {
			if (((SQLCatalog) value).getNativeTerm().equals("owner")) {
				setIcon(ownerIcon);
			} else if (((SQLCatalog) value).getNativeTerm().equals("database")) {
				setIcon(dbIcon);
			} else if (((SQLCatalog) value).getNativeTerm().equals("schema")) {
				setIcon(schemaIcon);
			} else {
				setIcon(cataIcon);
			}
		} else if (value instanceof SQLSchema) {
			if (((SQLSchema) value).getNativeTerm().equals("owner")) {
				setIcon(ownerIcon);
			} else {
				setIcon(schemaIcon);
			}
		} else if (value instanceof SQLTable) {
            
			SQLTable table = (SQLTable) value;
            if (session.getProfileManager().getResults(table).size() > 0) {
                setIcon(tableProfiledIcon);
            } else {
                setIcon(tableIcon);
            }
            if ((table).getObjectType() != null) {
			    setText((table).getName()+" ("+(table).getObjectType()+")");
			} else {
			    setText((table).getName());
			}
		} else if (value instanceof SQLRelationship) {
            //XXX ARRRRRRGGGGGHHHHHHH!!!! No way of knowing which end of a relationship we're
            // looking at because the relationship has two parents.  Maybe able to do it with the row number.
            if (true) {
                setIcon(exportedKeyIcon);
            } else {
                setIcon(importedKeyIcon);
            }
		} else if (value instanceof SQLIndex) {
            SQLIndex i = (SQLIndex) value;
            if (i.isPrimaryKeyIndex()) {
                setIcon(pkIndexIcon);
            } else if (i.isUnique()) {
                setIcon(uniqueIndexIcon);
            } else {
                setIcon(indexIcon);
            }
        } else if (value instanceof SQLColumn || value instanceof Column) {
            setIcon(columnIcon);
        } else {
			setIcon(null);
		}

		this.selected = sel;
		this.hasFocus = hasFocus;

		if (value instanceof SQLObject) {
		    if (((SQLObject) value).isPopulated()) {
		        setForeground(Color.black);
		    } else {
		        setForeground(Color.lightGray);
		    }
		}
		return this;
	}
}