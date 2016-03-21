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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;


public class MinimalCakeGenerator extends AnAction {

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
    if (virtualFile == null || !virtualFile.getPath().endsWith(".scala")) {
      return;
    }

    String contents = createText(document, e);

    updateText(contents, project, document);
  }

  private String createText(Document document, AnActionEvent e) {
    return document.getText() + generatePatternStrings(getPsiClassFromContext(e));
  }

  private void updateText(String contents, Project project, Document document) {
    final Runnable readRunner = () -> document.setText(contents);
    ApplicationManager.getApplication().invokeLater(() ->
        CommandProcessor.getInstance().executeCommand(
            project,
            () -> ApplicationManager.getApplication().runWriteAction(readRunner),
            "MinimalCakeGenerate",
            null
        )
    );
  }


  private PsiClass getPsiClassFromContext(AnActionEvent e) {
    PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
    Editor editor = e.getData(PlatformDataKeys.EDITOR);

    if (psiFile == null || editor == null) {
      return null;
    }

    int offset = editor.getCaretModel().getOffset();
    PsiElement element = psiFile.findElementAt(offset);

    return PsiTreeUtil.getParentOfType(element, PsiClass.class);
  }

  private String generatePatternStrings(PsiClass psiClass) {
    String className = psiClass.getName();
    String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);

    return String.format(
        "trait Uses%s {\n\tval %s: %s\n}\n\n" +
            "trait MixIn%s {\n\tval %s: %s = new %sImpl\n}\n\n" +
            "class %sImpl extends %s",
        className, fieldName, className,
        className, fieldName, className, className,
        className, className
    );
  }

}
