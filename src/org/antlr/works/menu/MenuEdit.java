/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.antlr.works.menu;

import org.antlr.works.components.grammar.CEditorGrammar;
import org.antlr.works.undo.Undo;

import javax.swing.text.DefaultEditorKit;

public class MenuEdit extends MenuAbstract {

    public MenuEdit(CEditorGrammar editor) {
        super(editor);
    }

    public void performUndo() {
        Undo undo = editor.getCurrentUndo();
        if(undo != null) {
            editor.disableTextPane(false);
            try {
                undo.performUndo();
            } finally {
                editor.enableTextPane(false);
                editor.changeUpdate();
            }
        }
    }

    public void performRedo() {
        Undo undo = editor.getCurrentUndo();
        if(undo != null) {
            editor.disableTextPane(false);
            try {
                undo.performRedo();
            } finally {
                editor.enableTextPane(false);
                editor.changeUpdate();
            }
        }
    }

    public void performCutToClipboard() {
        getTextPane().getActionMap().get(DefaultEditorKit.cutAction).actionPerformed(null);
    }

    public void performCopyToClipboard() {
        getTextPane().getActionMap().get(DefaultEditorKit.copyAction).actionPerformed(null);
    }

    public void performPasteFromClipboard() {
        getTextPane().getActionMap().get(DefaultEditorKit.pasteAction).actionPerformed(null);
    }

    public void performSelectAll() {
        getTextPane().getActionMap().get(DefaultEditorKit.selectAllAction).actionPerformed(null);
    }

}