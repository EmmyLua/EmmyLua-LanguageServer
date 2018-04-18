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
import com.tang.intellij.lua.psi.Visibility;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public class LuaDocFieldDefImpl extends ASTWrapperPsiElement implements LuaDocFieldDef {

  public LuaDocFieldDefImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull LuaDocVisitor visitor) {
    visitor.visitFieldDef(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaDocVisitor) accept((LuaDocVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public LuaDocAccessModifier getAccessModifier() {
    return findChildByClass(LuaDocAccessModifier.class);
  }

  @Override
  @Nullable
  public LuaDocClassNameRef getClassNameRef() {
    return findChildByClass(LuaDocClassNameRef.class);
  }

  @Override
  @Nullable
  public LuaDocCommentString getCommentString() {
    return findChildByClass(LuaDocCommentString.class);
  }

  @Override
  @Nullable
  public LuaDocTy getTy() {
    return findChildByClass(LuaDocTy.class);
  }

  @Override
  @Nullable
  public PsiElement getId() {
    return findChildByType(ID);
  }

  @NotNull
  public ITy guessParentType(SearchContext context) {
    return LuaDocPsiImplUtilKt.guessParentType(this, context);
  }

  @NotNull
  public Visibility getVisibility() {
    return LuaDocPsiImplUtilKt.getVisibility(this);
  }

  @Nullable
  public PsiElement getNameIdentifier() {
    return LuaDocPsiImplUtilKt.getNameIdentifier(this);
  }

  @NotNull
  public PsiElement setName(String newName) {
    return LuaDocPsiImplUtilKt.setName(this, newName);
  }

  @Nullable
  public String getName() {
    return LuaDocPsiImplUtilKt.getName(this);
  }

  public int getTextOffset() {
    return LuaDocPsiImplUtilKt.getTextOffset(this);
  }

  @Nullable
  public String getFieldName() {
    return LuaDocPsiImplUtilKt.getFieldName(this);
  }

}
