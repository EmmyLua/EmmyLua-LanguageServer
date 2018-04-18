// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;

public interface LuaNameExpr extends LuaExpr, PsiNameIdentifierOwner, LuaModuleClassField {

  @NotNull
  PsiElement getId();

  @NotNull
  PsiElement setName(String name);

  @NotNull
  String getName();

  @NotNull
  PsiElement getNameIdentifier();

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaNameExpr, ...)
  //methods are not found in LuaPsiImplUtilKt

  @NotNull
  PsiReference[] getReferences();

  //WARNING: toString(...) is skipped
  //matching toString(LuaNameExpr, ...)
  //methods are not found in LuaPsiImplUtilKt

}
