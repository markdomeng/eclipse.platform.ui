/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.dnd;

import org.eclipse.ui.IEditorPart;

/**
 * @since 3.0
 */
public class EditorDragSource extends TestDragSource {

    int editorIdx;

    boolean wholeFolder;

    /**
     * @param editor
     * @param wholeFolder
     */
    public EditorDragSource(int editorIdx, boolean wholeFolder) {
        super();
        this.editorIdx = editorIdx;
        this.wholeFolder = wholeFolder;
    }

    IEditorPart getPart() {
        return getPage().getEditors()[editorIdx];
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.tests.dnd.TestDragSource#getName()
     */
    public String toString() {
        String title = "editor " + editorIdx;

        if (wholeFolder) {
            return title + " folder";
        }
        return title;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.tests.dnd.TestDragSource#drag(org.eclipse.swt.graphics.Point)
     */
    public void drag(AbstractTestDropTarget target) {
        DragOperations.drag(getPart(), target, wholeFolder);
    }

}