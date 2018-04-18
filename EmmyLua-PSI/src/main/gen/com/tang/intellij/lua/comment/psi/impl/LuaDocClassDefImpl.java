// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.tang.intellij.lua.comment.psi.LuaDocTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.tang.intellij.lua.comment.psi.*;
import com.tang.intellij.lua.ty.ITyClass;

public class LuaDocClassDefImpl extends ASTWrapperPsiElement implements LuaDocClassDef {

  public LuaDocClassDefImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull LuaDocVisitor visitor) {
    visitor.visitClassDef(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaDocVisitor) accept((LuaDocVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public LuaDocCommentString getCommentString() {
    return findChildByClass(LuaDocCommentString.class);
  }

  @Override
  @NotNull
  public PsiElement getId() {
    return findNotNullChildByType(ID);
  }

  @NotNull
  public ITyClass getType() {
    return LuaDocPsiImplUtilKt.getType(this);
  }

  @NotNull
  public PsiElement getNameIdentifier() {
    return LuaDocPsiImplUtilKt.getNameIdentifier(this);
  }

  @NotNull
  public PsiElement setName(String newName) {
    return LuaDocPsiImplUtilKt.setName(this, newName);
  }

  @NotNull
  public String getName() {
    return LuaDocPsiImplUtilKt.getName(this);
  }

  public int getTextOffset() {
    return LuaDocPsiImplUtilKt.getTextOffset(this);
  }

  @Override
  @Nullable
  public LuaDocClassNameRef getSuperClassNameRef() {
    return findChildByClass(LuaDocClassNameRef.class);
  }

  @Override
  @Nullable
  public PsiElement getModule() {
    return findChildByType(TAG_MODULE);
  }

}
