// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.tang.intellij.lua.psi.LuaClassField;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.tang.intellij.lua.psi.Visibility;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public interface LuaDocFieldDef extends LuaClassField, LuaDocPsiElement, PsiNameIdentifierOwner {

  @Nullable
  LuaDocAccessModifier getAccessModifier();

  @Nullable
  LuaDocClassNameRef getClassNameRef();

  @Nullable
  LuaDocCommentString getCommentString();

  @Nullable
  LuaDocTy getTy();

  @Nullable
  PsiElement getId();

  @NotNull
  ITy guessParentType(SearchContext context);

  @NotNull
  Visibility getVisibility();

  @Nullable
  PsiElement getNameIdentifier();

  @NotNull
  PsiElement setName(String newName);

  @Nullable
  String getName();

  int getTextOffset();

  @Nullable
  String getFieldName();

  //WARNING: toString(...) is skipped
  //matching toString(LuaDocFieldDef, ...)
  //methods are not found in LuaDocPsiImplUtilKt

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaDocFieldDef, ...)
  //methods are not found in LuaDocPsiImplUtilKt

}
