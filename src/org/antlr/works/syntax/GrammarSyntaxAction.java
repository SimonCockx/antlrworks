package org.antlr.works.syntax;

import org.antlr.works.ate.folding.ATEFoldingEntity;
import org.antlr.works.ate.syntax.misc.ATEToken;
import org.antlr.works.editor.EditorPersistentObject;
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

public class GrammarSyntaxAction extends GSScopable implements EditorPersistentObject, ATEFoldingEntity {

    public GrammarSyntaxRule rule;
    private ATEToken start;
    public ATEToken end;
    public int actionNum;
    private boolean expanded = true;

    public GrammarSyntaxAction(GrammarSyntaxRule rule, ATEToken start) {
        this.rule = rule;
        this.start = start;
    }

    public boolean containsIndex(int index) {
        return index >= start.getStartIndex() && index <= end.getEndIndex();
    }

    public boolean equals(Object other) {
        return getUniqueIdentifier() == ((GrammarSyntaxAction)other).getUniqueIdentifier();
    }

    public int getUniqueIdentifier() {
        String actionText = start.getText().substring(start.start, end.end);
        return (rule.name+actionText+actionNum).hashCode();
    }

    public void foldingEntitySetExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean foldingEntityIsExpanded() {
        return expanded;
    }

    public boolean foldingEntityCanBeCollapsed() {
        return true;
    }

    public int foldingEntityGetStartParagraphIndex() {
        return start.getStartIndex();
    }

    public int foldingEntityGetStartIndex() {
        return start.getStartIndex();
    }

    public int foldingEntityGetEndIndex() {
        return end.getEndIndex();
    }

    public int foldingEntityGetStartLine() {
        return start.startLineNumber;
    }

    public int foldingEntityGetEndLine() {
        return end.endLineNumber;
    }

    public String foldingEntityPlaceholderString() {
        return "{ ... }";
    }

    public String foldingEntityID() {
        return String.valueOf(getUniqueIdentifier());
    }

    public int foldingEntityLevel() {
        return 1;
    }

    public Object getPersistentID() {
        return getUniqueIdentifier();
    }

    public void persistentAssign(EditorPersistentObject otherObject) {
        GrammarSyntaxAction otherAction = (GrammarSyntaxAction)otherObject;
        this.expanded = otherAction.expanded;
    }
}
