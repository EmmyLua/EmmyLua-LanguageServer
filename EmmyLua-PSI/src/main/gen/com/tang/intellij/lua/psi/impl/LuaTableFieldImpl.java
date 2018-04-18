// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.tang.intellij.lua.psi.LuaTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.tang.intellij.lua.psi.*;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;

public class LuaTableFieldImpl extends ASTWrapperPsiElement implements LuaTableField {

  public LuaTableFieldImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull LuaVisitor visitor) {
    visitor.visitTableField(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaVisitor) accept((LuaVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<LuaExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, LuaExpr.class);
  }

  @Override
  @Nullable
  public PsiElement getId() {
    return findChildByType(ID);
  }

  @Nullable
  public PsiElement getNameIdentifier() {
    return LuaPsiImplUtilKt.getNameIdentifier(this);
  }

  @NotNull
  public PsiElement setName(String name) {
    return LuaPsiImplUtilKt.setName(this, name);
  }

  @Nullable
  public String getName() {
    return LuaPsiImplUtilKt.getName(this);
  }

  public int getTextOffset() {
    return LuaPsiImplUtilKt.getTextOffset(this);
  }

  @Nullable
  public String getFieldName() {
    return LuaPsiImplUtilKt.getFieldName(this);
  }

  @NotNull
  public ITy guessParentType(SearchContext context) {
    return LuaPsiImplUtilKt.guessParentType(this, context);
  }

  @NotNull
  public Visibility getVisibility() {
    return LuaPsiImplUtilKt.getVisibility(this);
  }

  @Nullable
  public LuaComment getComment() {
    return LuaPsiImplUtilKt.getComment(this);
  }

  @Nullable
  public LuaExpr getIdExpr() {
    return LuaPsiImplUtilKt.getIdExpr(this);
  }

  @Override
  @Nullable
  public PsiElement getLbrack() {
    return findChildByType(LBRACK);
  }

}
