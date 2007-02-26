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

package org.antlr.works.syntax;

import org.antlr.works.ate.syntax.generic.ATESyntaxParser;
import org.antlr.works.ate.syntax.misc.ATEScope;
import org.antlr.works.ate.syntax.misc.ATEToken;

import java.util.*;

public class GrammarSyntaxParser extends ATESyntaxParser {

    public static final String BEGIN_GROUP = "// $<";
    public static final String END_GROUP = "// $>";

    public static final String TOKENS_BLOCK_NAME = "tokens";
    public static final String OPTIONS_BLOCK_NAME = "options";
    public static final String PARSER_HEADER_BLOCK_NAME = "@header";
    public static final String LEXER_HEADER_BLOCK_NAME = "@lexer::header";
    public static final String PARSER_MEMBERS_BLOCK_NAME = "@members";
    public static final String LEXER_MEMBERS_BLOCK_NAME = "@lexer::members";

    public static final List<String> blockIdentifiers;
    public static final List<String> ruleModifiers;
    public static final List<String> keywords;
    public static final List<String> predefinedReferences;

    public List<GrammarSyntaxRule> rules = new ArrayList<GrammarSyntaxRule>();
    public List<GrammarSyntaxGroup> groups = new ArrayList<GrammarSyntaxGroup>();
    public List<GrammarSyntaxBlock> blocks = new ArrayList<GrammarSyntaxBlock>();         // tokens {}, options {}
    public List<GrammarSyntaxAction> actions = new ArrayList<GrammarSyntaxAction>();        // { action } in rules
    public List<GrammarSyntaxReference> references = new ArrayList<GrammarSyntaxReference>();
    public List<ATEToken> decls = new ArrayList<ATEToken>();

    public GrammarSyntaxName name;

    static {
        blockIdentifiers = new ArrayList<String>();
        blockIdentifiers.add(OPTIONS_BLOCK_NAME);
        blockIdentifiers.add(TOKENS_BLOCK_NAME);
        blockIdentifiers.add(PARSER_HEADER_BLOCK_NAME);
        blockIdentifiers.add(LEXER_HEADER_BLOCK_NAME);
        blockIdentifiers.add(PARSER_MEMBERS_BLOCK_NAME);
        blockIdentifiers.add(LEXER_MEMBERS_BLOCK_NAME);

        ruleModifiers = new ArrayList<String>();
        ruleModifiers.add("protected");
        ruleModifiers.add("public");
        ruleModifiers.add("private");
        ruleModifiers.add("fragment");

        keywords = new ArrayList<String>();
        keywords.addAll(blockIdentifiers);
        keywords.addAll(ruleModifiers);
        keywords.add("returns");
        keywords.add("init");

        predefinedReferences = new ArrayList<String>();
        predefinedReferences.add("EOF");
    }

    public GrammarSyntaxParser() {
    }

    @Override
    public void parseTokens() {
        rules.clear();
        groups.clear();
        blocks.clear();
        actions.clear();
        references.clear();
        decls.clear();

        while(nextToken()) {

            if(isComplexComment(0)) continue;

            if(tryMatchName()) continue;
            if(tryMatchScope()) continue; // scope before block
            if(tryMatchBlock()) continue;
            if(tryMatchRule()) continue;

            if(isSingleComment(0)) {
                GrammarSyntaxGroup group = matchRuleGroup(rules);
                if(group != null)
                    groups.add(group);
            }
        }
    }

    private boolean tryMatchName() {
        mark();
        GrammarSyntaxName n = matchName();
        if(n != null) {
            name = n;
            return true;
        } else {
            rewind();
            return false;
        }
    }

    private boolean tryMatchBlock() {
        mark();
        GrammarSyntaxBlock block = matchBlock();
        if(block != null) {
            blocks.add(block);
            return true;
        } else {
            rewind();
            return false;
        }
    }

    private boolean tryMatchScope() {
        mark();
        if(matchScope()) {
            return true;
        } else {
            rewind();
            return false;
        }
    }

    private boolean tryMatchRule() {
        mark();
        GrammarSyntaxRule rule = matchRule(actions, references);
        if(rule != null) {
            rules.add(rule);
            return true;
        } else {
            rewind();
            return false;
        }
    }

    /**
     * Matches the name of the grammar:
     *
     * grammar lexer JavaLexer;
     *
     */
    private GrammarSyntaxName matchName() {
        if(isID(0, "grammar")) {
            ATEToken start = T(0);
            nextToken(); // skip 'grammar'

            // Check if the grammar has a type (e.g. lexer, parser, tree, etc)
            ATEToken type = null;
            if(GrammarSyntaxName.isKnownType(T(0).getAttribute())) {
                type = T(0);
                nextToken(); // skip the type
            }

            // After the type comes the name of the grammar
            ATEToken name = T(0);

            // Loop until we find the semi colon
            while(nextToken()) {
                if(isSEMI(0)) {
                    // semi colon found, the grammar name is matched!
                    return new GrammarSyntaxName(name, start, T(0), type);
                }
            }
        }

        return null;
    }

    /**
     * Matches a scope:
     *
     * scope [name] ( BLOCK | ';' )
     *
     * where
     *  BLOCK = { ... }
     *
     */
    // todo ut
    private boolean matchScope() {
        // Must begin with the keyword 'scope'
        ATEToken start = T(0);
        if(isID(0, "scope")) {
            if(!nextToken()) return false;
        } else {
            return false;
        }

        // Match the optional name
        if(isID(0)) {
            if(!nextToken()) return false;
        }

        // Match either the block or the semi
        if(isOpenBLOCK(0)) {
            ATEToken beginBlock = T(0);
            if(matchBalancedToken("{", "}")) {
                beginBlock.type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
                T(0).type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
                start.type = GrammarSyntaxLexer.TOKEN_BLOCK_LABEL;
            } else {
                return false;
            }
        } else {
            return isSEMI(0);
        }

        return true;
    }

    /**
     * Matches a block:
     *
     * LABEL BLOCK
     *
     * where
     *  LABEL = @id || @bar::foo | label
     *  BLOCK = { ... }
     *
     */
    private GrammarSyntaxBlock matchBlock() {
        ATEToken start = T(0);
        int startIndex = getPosition();
        if(isID(0) || isTokenType(0, GrammarSyntaxLexer.TOKEN_BLOCK_LABEL)) {
            if(!nextToken()) return null;
        } else {
            return null;
        }

        // check that the name of the block is known
        String blockName = start.getAttribute().toLowerCase();

        ATEToken beginBlock = T(0);
        if(matchBalancedToken("{", "}")) {
            beginBlock.type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
            T(0).type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
            start.type = GrammarSyntaxLexer.TOKEN_BLOCK_LABEL;
        } else {
            return null;
        }

        GrammarSyntaxBlock block = new GrammarSyntaxBlock(blockName, start, T(0), getTokens().subList(startIndex, getPosition()));
        block.parse();
        if(block.isTokenBlock) {
            List<ATEToken> tokens = block.getDeclaredTokens();
            for(int i=0; i<tokens.size(); i++) {
                ATEToken lexerToken = tokens.get(i);
                lexerToken.type = GrammarSyntaxLexer.TOKEN_DECL;
                decls.add(lexerToken);
            }
        }
        return block;
    }

    /**
     * Matches a rule:
     *
     * MODIFIER? ruleNameID ARG? '!'? COMMENT*
     *
     * where
     *  MODIFIER = protected | public | private | fragment
     *  COMMENT = // or /*
     *  ARG = '[' Type arg... ']'
     *
     */
    public GrammarSyntaxRule matchRule(List<GrammarSyntaxAction> actions, List<GrammarSyntaxReference> references) {
        ATEToken start = T(0);

        // Match any modifiers
        if(ruleModifiers.contains(T(0).getAttribute())) {
            // skip the modifier
            if(!nextToken()) return null;
        }

        // Match the name (it has to be an ID)
        if(!isID(0)) return null;

        GrammarSyntaxToken tokenName = (GrammarSyntaxToken) T(0);
        String name = tokenName.getAttribute();
        if(!nextToken()) return null;

        // Match any argument
        if(matchArguments()) {
            if(!nextToken()) return null;
        }

        // Match any optional "!"
        if(T(0).getAttribute().equals("!")) {
            // skip it
            if(!nextToken()) return null;
        }

        // Match any comments
        while(isSingleComment(0) || isComplexComment(0)) {
            if(!nextToken()) return null;
        }

        // Matches any number of scopes and blocks
        while(true) {
            if(matchScope()) {
                if(!nextToken()) return null;
                continue;
            }
            if(matchBlock() != null) {
                if(!nextToken()) return null;
                continue;
            }

            if(isCOLON(0)) {
                // When a colon is matched, we are at the beginning of the content of the rule
                break;
            } else {
                // Invalid rule matching
                return null;
            }
        }

        // Parse the content of the rule (after the ':')
        final ATEToken colonToken = T(0);
        final GrammarSyntaxRule rule = new GrammarSyntaxRule(this, name, start, colonToken, null);
        final int refOldSize = references.size();
        LabelScope labels = new LabelScope();
        labels.begin();
        while(nextToken()) {
            if(isSEMI(0)) {
                // End of the rule.
                matchRuleExceptionGroup();

                // Record the token that defines the end of the rule
                rule.end = T(0);

                // Change the token type of the name
                tokenName.type = GrammarSyntaxLexer.TOKEN_DECL;
                decls.add(tokenName);

                // Each rule contains the index of its references. It is used when refactoring.
                if(references.size() > refOldSize) {
                    // If the size of the references array has changed, then we have some references
                    // inside this rule. Sets the indexes into the rule.
                    rule.setReferencesIndexes(refOldSize, references.size()-1);
                }

                // Indicate to the rule that is has been parsed completely.
                rule.completed();

                // Return the rule
                return rule;
            } else if(isID(0)) {
                // Probably a reference inside the rule.

                // Check for label first:
                //   label=reference
                //   label+=reference
                //   label='string'

                if(isChar(1, "=")) {
                    T(0).type = GrammarSyntaxLexer.TOKEN_LABEL;
                    labels.add(T(0).getAttribute());
                    if(!skip(2)) return null;
                } else if(isChar(1, "+") && isChar(2, "=")) {
                    T(0).type = GrammarSyntaxLexer.TOKEN_LABEL;
                    labels.add(T(0).getAttribute());
                    if(!skip(3)) return null;
                }

                // Skip if the operand is not an ID. Can be a string for example, as in:
                // label='operand'
                if(!isID(0)) continue;

                // Ignore reserved keywords
                ATEToken refToken = T(0);
                if(keywords.contains(refToken.getAttribute())) {
                    continue;
                }

                // Match any option arguments
                if(isChar(1, "[")) {
                    if(!nextToken()) return null;
                    if(matchArguments()) {
                        // do not advance one token because we are at the end of parsing the reference
                        //if(!nextToken()) return null;
                    }
                }

                // Now we have the reference token. Set the token flags
                if(labels.lookup(refToken.getAttribute())) {
                    // Reference is to a label, not a lexer/parser rule
                    refToken.type = GrammarSyntaxLexer.TOKEN_LABEL;
                } else {
                    refToken.type = GrammarSyntaxLexer.TOKEN_REFERENCE;
                    // Create and add the new reference
                    references.add(new GrammarSyntaxReference(rule, refToken));
                }
            } else if(isOpenBLOCK(0)) {
                // Match an action

                ATEToken t0 = T(0);
                GrammarSyntaxAction action = new GrammarSyntaxAction(rule, t0);
                if(matchBalancedToken("{", "}", action)) {
                    t0.type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
                    T(0).type = GrammarSyntaxLexer.TOKEN_BLOCK_LIMIT;
                    action.end = T(0);
                    action.actionNum = actions.size();
                    action.setScope(rule);
                    actions.add(action);
                }
            } else if(isTokenType(0, GrammarSyntaxLexer.TOKEN_REWRITE)) {
                // Match a rewrite syntax beginning with ->
                if(!nextToken()) return null;

                matchRewriteSyntax();
            } else if(isLPAREN(0)) {
                labels.begin();
            } else if(isRPAREN(0)) {
                labels.end();
            }
        }

        return null;
    }

    private class LabelScope {

        Stack<Set<String>> labels = new Stack<Set<String>>();

        public void begin() {
            labels.push(new HashSet<String>());
        }

        public void end() {
            // todo ask Terence is label are scoped
            //labels.pop();
        }

        public void add(String label) {
            if(labels.isEmpty()) {
                System.err.println("[LabelScope] Stack is empty");
                return;
            }
            labels.peek().add(label);
        }

        public boolean lookup(String label) {
            for(int i=0; i<labels.size(); i++) {
                if(labels.get(i).contains(label)) return true;
            }
            return false;
        }
    }

    private boolean matchArguments() {
        return isChar(0, "[") && matchBalancedToken("[", "]");
    }

    public void matchRuleExceptionGroup() {
        if(!matchOptional("exception"))
            return;

        // Optional ARG_ACTION
        if(isOpenBLOCK(1))
            nextToken();

        while(matchOptional("catch")) {
            nextToken();    // ARG_ACTION: []
            nextToken();    // ACTION: { }
        }
    }

    public void matchRewriteSyntax() {
        /*  if(isOpenBLOCK(0) && isChar(1, "?") && isID(2) && isLPAREN(3)) {
          // -> { condition }? foo()
          skip(3);
          matchBalancedToken("(", ")");
          return;
      }

      if(isID(0) && isLPAREN(1)) {
          if(isID(0, "template")) {
              // -> template(...) ["asd" | << asd >>]
              skip(1);
              matchBalancedToken("(", ")");

              skip(1);
              if(isTokenType(0, ATESyntaxLexer.TOKEN_DOUBLE_QUOTE_STRING)) {
                  // case with "asd"

                  // Set the token type to ST_STRING in order to avoid confusion between this type of string
                  // and the normal string in the grammar (ANTLR 3 does not like double-quote string in grammar
                  // except for inline template).
                  T(0).type = GrammarSyntaxLexer.TOKEN_ST_STRING;
              } else {
                  // try to match case with << asd >>
                  if(isChar(0, "<") && isChar(1, "<")) {
                      matchBalancedToken("<", ">");
                  }
              }
              return;
          } else {
              // -> foo(...)
              skip(1);
              matchBalancedToken("(", ")");
              return;
          }
      }

      if(isOpenBLOCK(0)) {
          // -> { new StringTemplate() }
          skip(0);
          return;
      }

      if(isID(0)) {
          // -> ASSIGN
          // Rewind one token because the next ID should not be skipped
          // otherwise it is not colored
          previousToken();
          return;
      }

      if(isChar(0, "$") && isID(1)) {
          // -> $e
          skip(1);
          return;
      }

      // Fall back if there is nothing after the rewrite ->
      previousToken();  */
    }

    /**
     * Matches all tokens until the balanced token's attribute is equal to close.
     *
     * @param open The open attribute
     * @param close The close attribute
     * @return true if the match succeeded
     */
    private boolean matchBalancedToken(String open, String close) {
        return matchBalancedToken(open, close, null);
    }

    private boolean matchBalancedToken(String open, String close, ATEScope scope) {
        T(0).scope = scope;
        int balance = 0;
        while(nextToken()) {
            String attr = T(0).getAttribute();
            T(0).scope = scope;
            if(attr.equals(open))
                balance++;
            else if(attr.equals(close)) {
                if(balance == 0) {
                    return true;
                }
                balance--;
            }
        }
        return false;
    }

    public GrammarSyntaxGroup matchRuleGroup(List<GrammarSyntaxRule> rules) {
        ATEToken token = T(0);
        String comment = token.getAttribute();

        if(comment.startsWith(BEGIN_GROUP)) {
            return new GrammarSyntaxGroup(comment.substring(BEGIN_GROUP.length(), comment.length()-1), rules.size()-1, token);
        } else if(comment.startsWith(END_GROUP)) {
            return new GrammarSyntaxGroup(rules.size()-1, token);
        } else
            return null;
    }

    public boolean matchOptional(String t) {
        if(isID(1, t)) {
            nextToken();
            return true;
        } else
            return false;
    }

    public boolean isLPAREN(int index) {
        // todo optimize
        return isChar(index, "(");
        //return isTokenType(index, GrammarSyntaxLexer.TOKEN_LPAREN);
    }

    public boolean isRPAREN(int index) {
        // todo optimize
        return isChar(index, ")");
        //return isTokenType(index, GrammarSyntaxLexer.TOKEN_LPAREN);
    }

    public boolean isSEMI(int index) {
        // todo optimize
        return isChar(index, ";");
//        return isTokenType(index, GrammarSyntaxLexer.TOKEN_SEMI);
    }

    public boolean isCOLON(int index) {
        // todo optimize
        return isChar(index, ":");
//        return isTokenType(index, GrammarSyntaxLexer.TOKEN_COLON);
    }

    public boolean isOpenBLOCK(int index) {
        // todo optimize
        return isChar(index, "{");
//        return isTokenType(index, GrammarSyntaxLexer.TOKEN_BLOCK);
    }

}
