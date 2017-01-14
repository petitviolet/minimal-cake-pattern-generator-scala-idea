import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;


public class ScalaMinimalCakeGenerator extends AnAction {
  private static final String DIALOG_TITLE = "Select mix-in type";
  private static final String LABEL_CLASS = "class";
  private static final String LABEL_OBJECT = "object";
  private static final int CHOICED_CLASS = DialogWrapper.OK_EXIT_CODE;
  private static final int CHOICED_OBJECT = DialogWrapper.CANCEL_EXIT_CODE;

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    final Document document = editor.getDocument();
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

    // only run in scala file
    if (virtualFile == null || !virtualFile.getPath().endsWith(".scala")) {
      return;
    }

    // update text with added boilerplate
    boolean choice = selectInsertType(project);
    // result contents
    String contents = createText(document, e, choice);
    updateText(contents, project, document);
  }

  private boolean selectInsertType(@Nullable Project project) {
    DialogBuilder builder = new DialogBuilder(project);
    builder.setTitle(DIALOG_TITLE);
    builder.removeAllActions();
    builder.addOkAction().setText(LABEL_CLASS);
    builder.addCancelAction().setText(LABEL_OBJECT);

    return builder.show() == CHOICED_CLASS;
  }

  /**
   * result string after execution this plugin
   */
  private String createText(Document document, AnActionEvent e, boolean choice) {
    return document.getText() + generateStrings(getPsiClass(e), choice);
  }

  /**
   * update text with created text includes boilerplate
   */
  private void updateText(String contents, Project project, Document document) {
    final Runnable readRunner = () -> document.setText(contents);
    ApplicationManager.getApplication().invokeLater(
        () -> CommandProcessor.getInstance()
            .executeCommand(project,
                () -> ApplicationManager.getApplication().runWriteAction(readRunner),
                "MinimalCakeGenerate",
                null)
    );
  }

  /**
   * extract PsiClass instance from AnActionEvent
   */
  private PsiClass getPsiClass(AnActionEvent e) {
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    Editor editor = e.getData(PlatformDataKeys.EDITOR);

    if (psiFile == null || editor == null) {
      return null;
    }

    int offset = editor.getCaretModel().getOffset();
    PsiElement element = psiFile.findElementAt(offset);

    return PsiTreeUtil.getParentOfType(element, PsiClass.class);
  }

  private String generateStrings(PsiClass psiClass, boolean choice) {
    if (choice) {
      return generateClassStrings(psiClass);
    } else {
      return generateObjectStrings(psiClass);
    }
  }

  /**
   * generate boilerplate string
   */
  private String generateClassStrings(PsiClass psiClass) {
    String className = psiClass.getName();
    String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);

    return String.format(
        "\ntrait Uses%s {\n\tval %s: %s\n}\n\n" +
            "trait MixIn%s {\n\tval %s: %s = new %sImpl\n}\n\n" +
            "private class %sImpl extends %s",
        className, fieldName, className,
        className, fieldName, className, className,
        className, className);
  }

  private String generateObjectStrings(PsiClass psiClass) {
    String className = psiClass.getName();
    String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);

    return String.format(
        "\ntrait Uses%s {\n\tval %s: %s\n}\n\n" +
            "trait MixIn%s {\n\tval %s: %s = %sImpl\n}\n\n" +
            "private object %sImpl extends %s",
        className, fieldName, className,
        className, fieldName, className, className,
        className, className);
  }

}
