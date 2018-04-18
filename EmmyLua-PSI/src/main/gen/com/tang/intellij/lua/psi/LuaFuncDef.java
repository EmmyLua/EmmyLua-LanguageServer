// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.ITyClass;

public interface LuaFuncDef extends LuaClassMethod, LuaDeclaration, LuaStatement {

  @Nullable
  LuaFuncBody getFuncBody();

  @Nullable
  PsiElement getId();

  @Nullable
  LuaComment getComment();

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaFuncDef, ...)
  //methods are not found in LuaPsiImplUtilKt

  @NotNull
  List<LuaParamNameDef> getParamNameDefList();

  @Nullable
  PsiElement getNameIdentifier();

  @NotNull
  PsiElement setName(String name);

  @Nullable
  String getName();

  int getTextOffset();

  //WARNING: toString(...) is skipped
  //matching toString(LuaFuncDef, ...)
  //methods are not found in LuaPsiImplUtilKt

  @NotNull
  ITy guessReturnType(SearchContext searchContext);

  @NotNull
  ITyClass guessParentType(SearchContext searchContext);

  @NotNull
  Visibility getVisibility();

  @NotNull
  LuaParamInfo[] getParams();

  @NotNull
  PsiReference[] getReferences();

}
