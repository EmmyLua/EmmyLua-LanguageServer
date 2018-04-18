// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.tang.intellij.lua.ty.ITyClass;

public interface LuaDocClassDef extends LuaDocPsiElement, PsiNameIdentifierOwner {

  @Nullable
  LuaDocCommentString getCommentString();

  @NotNull
  PsiElement getId();

  @NotNull
  ITyClass getType();

  //WARNING: getPresentation(...) is skipped
  //matching getPresentation(LuaDocClassDef, ...)
  //methods are not found in LuaDocPsiImplUtilKt

  @NotNull
  PsiElement getNameIdentifier();

  @NotNull
  PsiElement setName(String newName);

  @NotNull
  String getName();

  int getTextOffset();

  //WARNING: toString(...) is skipped
  //matching toString(LuaDocClassDef, ...)
  //methods are not found in LuaDocPsiImplUtilKt

  @Nullable
  LuaDocClassNameRef getSuperClassNameRef();

  @Nullable
  PsiElement getModule();

}
