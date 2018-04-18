// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public interface LuaTableField extends LuaClassField, PsiNameIdentifierOwner, LuaCommentOwner {

  @NotNull
  List<LuaExpr> getExprList();

  @Nullable
  PsiElement getId();

  @Nullable
  PsiElement getNameIdentifier();

  @NotNull
  PsiElement setName(String name);

  @Nullable
  String getName();

  int getTextOffset();

  //WARNING: toString(...) is skipped
  //matching toString(LuaTableField, ...)
  //methods are not found in LuaPsiImplUtilKt

  @Nullable
  String getFieldName();

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaTableField, ...)
  //methods are not found in LuaPsiImplUtilKt

  @NotNull
  ITy guessParentType(SearchContext context);

  @NotNull
  Visibility getVisibility();

  @Nullable
  LuaComment getComment();

  @Nullable
  LuaExpr getIdExpr();

  @Nullable
  PsiElement getLbrack();

}
