/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Contributor(s):
 *   Norris Boyd
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript;

/**
 * This class implements iterator objects. See 
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Iterators
 *
 * @author Norris Boyd
 */
public final class NativeIterator extends IdScriptableObject {
    private static final Object ITERATOR_TAG = new Object();
    
    static void init(ScriptableObject scope, boolean sealed) {
        // Iterator
        NativeIterator iterator = new NativeIterator();
        iterator.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
        
        // Generator
        NativeGenerator prototype = NativeGenerator.init(scope, sealed);

        // StopIteration
        NativeObject obj = new StopIteration();
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) { obj.sealObject(); }
        ScriptableObject.defineProperty(scope, STOP_ITERATION, obj,
                                        ScriptableObject.DONTENUM);
    }
    
    /**
     * Only for constructing the prototype object.
     */
    private NativeIterator() {
    }
    
    private NativeIterator(Object objectIterator, boolean keyOnly) {
      this.objectIterator = objectIterator;
      this.keyOnly = keyOnly;
    }
    
    public static final String STOP_ITERATION = "StopIteration";
    public static final String ITERATOR_PROPERTY_NAME = "__iterator__";
    
    static class StopIteration extends NativeObject {
        public String getClassName() { return STOP_ITERATION; }
    }

    public String getClassName() {
        return "Iterator";
    }

    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
          case Id_constructor:    arity=2; s="constructor";          break;
          case Id_next:           arity=0; s="next";                 break;
          case Id___iterator__:   arity=1; s=ITERATOR_PROPERTY_NAME; break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(ITERATOR_TAG, id, s, arity);
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(ITERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        
        if (id == Id_constructor) {
            return jsConstructor(cx, scope, thisObj, args);
        }

        if (!(thisObj instanceof NativeIterator))
            throw incompatibleCallError(f);
        
        NativeIterator iterator = (NativeIterator) thisObj;
        
        switch (id) {

          case Id_next: 
            return iterator.next(cx, scope);

          case Id___iterator__:
            /// XXX: what about argument? SpiderMonkey apparently ignores it
            return thisObj;

          default: 
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }
    
    /* the javascript constructor */
    private static Object jsConstructor(Context cx, Scriptable scope, 
                                        Scriptable thisObj, Object[] args)
    {
        if (args.length == 0 || args[0] == null || 
            args[0] == Undefined.instance)
        {
            throw ScriptRuntime.typeError1("msg.no.properties", 
                                           ScriptRuntime.toString(args[0]));
        }
        Scriptable obj = ScriptRuntime.toObject(scope, args[0]);
        boolean keyOnly = args.length > 1 && ScriptRuntime.toBoolean(args[1]);
        if (thisObj != null) {
            // Called as a function. Convert to iterator if possible.
            Scriptable iterator = ScriptRuntime.toIterator(cx, scope, obj, 
                                                           keyOnly);
            if (iterator != null) {
                return iterator;
            }
        }
        
        // Otherwise, just set up to iterate over the properties of the object.
        Object objectIterator = ScriptRuntime.enumInit(obj, cx, false);
        ScriptRuntime.setEnumNumbers(objectIterator, true);
        NativeIterator result = new NativeIterator(objectIterator, keyOnly);
        result.setPrototype(NativeIterator.getClassPrototype(scope, 
                                result.getClassName()));
        result.setParentScope(scope);
        return result;
    }
    
    private Object next(Context cx, Scriptable scope) {
        Boolean b = ScriptRuntime.enumNext(this.objectIterator);
        if (b.booleanValue() == false) {
            // Out of values. Throw StopIteration.
            Scriptable top = ScriptableObject.getTopLevelScope(scope);
            Object e = top.get(STOP_ITERATION, scope);
            throw new JavaScriptException(e, null, 0);
        }
        Object id = ScriptRuntime.enumId(this.objectIterator, cx);
        if (this.keyOnly) {
            return id;
        }
        Object value = ScriptRuntime.enumValue(this.objectIterator, cx);
        Object[] elements = { id, value };
        return cx.newArray(scope, elements);
    }
    
// #string_id_map#

    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2007-06-11 09:43:19 EDT
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==4) { X="next";id=Id_next; }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            else if (s_length==12) { X="__iterator__";id=Id___iterator__; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor           = 1,
        Id_next                  = 2,
        Id___iterator__          = 3,
        MAX_PROTOTYPE_ID         = 3;

// #/string_id_map#

    private Object objectIterator;
    private boolean keyOnly;
}
