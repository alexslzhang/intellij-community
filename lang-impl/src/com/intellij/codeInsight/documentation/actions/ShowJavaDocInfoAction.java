package com.intellij.codeInsight.documentation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;

public class ShowJavaDocInfoAction extends BaseCodeInsightAction implements HintManager.ActionToIgnore {
  public ShowJavaDocInfoAction() {
    setEnabledInModalContext(true);
    setInjectedContext(true);
  }

  protected CodeInsightActionHandler getHandler() {
    return new CodeInsightActionHandler() {
      public void invoke(Project project, Editor editor, PsiFile file) {
        DocumentationManager.getInstance(project).showJavaDocInfo(editor, file, true);
      }

      public boolean startInWriteAction() {
        return false;
      }
    };
  }


  protected boolean isValidForLookup() {
    return true;
  }

  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();

    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
    PsiElement element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
    if (editor == null && element == null) {
      presentation.setEnabled(false);
      return;
    }

    if (LookupManager.getInstance(project).getActiveLookup() != null) {
      if (!isValidForLookup()) {
        presentation.setEnabled(false);
      }
      else {
        presentation.setEnabled(true);
      }
    }
    else {
      if (editor != null) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
          presentation.setEnabled(false);
        }

        if (element == null && file != null) {
          if (file.getLanguage() instanceof XMLLanguage) {
            // we allow request quick doc over content of the tag
            final PsiElement contextElement = file.findElementAt(editor.getCaretModel().getOffset());
            element = PsiTreeUtil.getParentOfType(contextElement, XmlTag.class);
          } else {
            final PsiReference ref = file.findReferenceAt(editor.getCaretModel().getOffset());
            if (ref instanceof PsiPolyVariantReference) {
              element = ref.getElement();
            }
          }
        }
      }

      if (element != null) {
        presentation.setEnabled(true);
      }
    }
  }

  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    final Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
    final PsiElement element = LangDataKeys.PSI_ELEMENT.getData(dataContext);

    if (project != null && editor != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickjavadoc");
      if (LookupManager.getInstance(project).getActiveLookup() != null) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickjavadoc.lookup");
      }
      actionPerformedImpl(project, editor);
    }
    else if (project != null) {
      if (DocumentationManager.getProviderFromElement(element) != null) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.quickjavadoc.ctrln");
        CommandProcessor.getInstance().executeCommand(project,
                                                      new Runnable() {
                                                        public void run() {
                                                          DocumentationManager.getInstance(project).showJavaDocInfo(element);
                                                        }
                                                      },
                                                      getCommandName(),
                                                      null);
      }
    }
  }
}