package com.intellij.uiDesigner.actions;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.uiDesigner.UIDesignerBundle;
import com.intellij.uiDesigner.propertyInspector.IntrospectedProperty;
import com.intellij.uiDesigner.propertyInspector.PropertyInspectorTable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class ShowJavadocAction extends AnAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.uiDesigner.actions.ShowJavadocAction");

  public void actionPerformed(final AnActionEvent e) {
    final PropertyInspectorTable inspector = (PropertyInspectorTable)e.getDataContext().getData(PropertyInspectorTable.class.getName());
    final IntrospectedProperty introspectedProperty = inspector.getSelectedIntrospectedProperty();
    final PsiClass aClass = inspector.getComponentClass();

    final PsiMethod getter = PropertyUtil.findPropertyGetter(aClass, introspectedProperty.getName(), false, true);
    LOG.assertTrue(getter != null);

    final PsiMethod setter = PropertyUtil.findPropertySetter(aClass, introspectedProperty.getName(), false, true);
    LOG.assertTrue(setter != null);

    final DocumentationManager documentationManager = DocumentationManager.getInstance(aClass.getProject());

    final DocumentationComponent component1 = new DocumentationComponent(documentationManager);
    final DocumentationComponent component2 = new DocumentationComponent(documentationManager);

    final TabbedPaneWrapper tabbedPane = new TabbedPaneWrapper();

    tabbedPane.addTab(UIDesignerBundle.message("tab.getter"), component1);
    tabbedPane.addTab(UIDesignerBundle.message("tab.setter"), component2);

    final JBPopup hint =
      JBPopupFactory.getInstance().createComponentPopupBuilder(tabbedPane.getComponent(), inspector)
        .setDimensionServiceKey(aClass.getProject(), DocumentationManager.JAVADOC_LOCATION_AND_SIZE, false)
        .setResizable(true)
        .setMovable(true)
        .setRequestFocus(true)
        .setTitle(UIDesignerBundle.message("property.javadoc.title", introspectedProperty.getName()))
        .createPopup();
    component1.setHint(hint);
    component2.setHint(hint);

    documentationManager.fetchDocInfo(getter, component1);
    documentationManager.queueFetchDocInfo(setter, component2);

    hint.show(new RelativePoint(inspector, new Point(0,0)));
    SwingUtilities.invokeLater(
      new Runnable() {
        public void run() {
          component1.requestFocus();
        }
      }
    );
  }

  public void update(final AnActionEvent e) {
    final PropertyInspectorTable inspector = (PropertyInspectorTable)e.getDataContext().getData(PropertyInspectorTable.class.getName());
    e.getPresentation().setEnabled(inspector != null &&
                                   inspector.getSelectedIntrospectedProperty() != null &&
                                   inspector.getComponentClass() != null);
  }
}
