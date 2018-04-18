// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public interface LuaClassMethodDef extends LuaClassMethod, LuaDeclaration, LuaStatement {

  @NotNull
  LuaClassMethodName getClassMethodName();

  @Nullable
  LuaFuncBody getFuncBody();

  @Nullable
  LuaComment getComment();

  @NotNull
  ITy guessParentType(SearchContext context);

  @NotNull
  Visibility getVisibility();

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
  //matching toString(LuaClassMethodDef, ...)
  //methods are not found in LuaPsiImplUtilKt

  @NotNull
  ITy guessReturnType(SearchContext searchContext);

  @NotNull
  LuaParamInfo[] getParams();

  boolean isStatic();

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaClassMethodDef, ...)
  //methods are not found in LuaPsiImplUtilKt

}
