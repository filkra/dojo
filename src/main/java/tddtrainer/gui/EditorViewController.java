package tddtrainer.gui;

import static tddtrainer.events.JavaCodeChangeEvent.CodeType.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.Random;
import java.util.ResourceBundle;

import de.hhu.krakowski.testresultview.data.TestResult;
import de.hhu.krakowski.testresultview.view.TestResultView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import tddtrainer.catalog.Exercise;
import tddtrainer.compiler.AutoCompilerResult;
import tddtrainer.events.ExerciseEvent;
import tddtrainer.events.JavaCodeChangeEvent;
import tddtrainer.events.automaton.ResetPhaseEvent;
import tddtrainer.events.automaton.SwitchedToGreenEvent;
import tddtrainer.events.automaton.SwitchedToRedEvent;
import tddtrainer.events.automaton.SwitchedToRefactorEvent;
import vk.core.api.CompileError;

public class EditorViewController extends SplitPane implements Initializable {

    private WebView tests;
    private WebView code;

    @FXML
    private TextArea testOutput;

    @FXML
    private TextArea testMessage;

    @FXML
    private AnchorPane codePane;

    @FXML
    private AnchorPane testPane;

    @FXML
    private Label codeLabel;

    @FXML
    private Label testLabel;

    @FXML
    private HBox iGreenBox;

    @FXML
    private Label iRedLabel1;

    @FXML
    private HBox codeBox;

    @FXML
    private TestResultView testresultview;

    @FXML
    HBox statuscontainer;

    @FXML
    Label status;

    Logger logger = LoggerFactory.getLogger(EditorViewController.class);

    String revertToCode;
    String revertToTest;
    private final EventBus bus;
    private int fontSize = 15;

    @Inject
    public EditorViewController(FXMLLoader loader, EventBus bus) {
        this.bus = bus;
        bus.register(this);
        URL resource = getClass().getResource("EditorView.fxml");
        loader.setLocation(resource);
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Error loading Root view", e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        iGreenBox.setVisible(false);
        addEditors();
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 5.0);
        AnchorPane.setRightAnchor(this, 5.0);
        AnchorPane.setTopAnchor(this, 60.0);

        testresultview.selectedResultProperty().addListener((observable, oldValue, newValue) -> {
            testMessage.setText(newValue.getDescription());
        });
    }

    @Subscribe
    public void showExercise(ExerciseEvent exerciseEvent) {
        Exercise exercise = exerciseEvent.getExercise();
        if (exercise != null) {
            showExercise(exercise);
        }
    }

    public String getCode() {
        return (String) code.getEngine().executeScript("editor.getValue()");
    }

    public String getTest() {
        return (String) tests.getEngine().executeScript("editor.getValue()");
    }

    public void compile(String type, String text) {
        JavaCodeChangeEvent.CodeType t = "code".equals(type) ? CODE : TEST;
        JavaCodeChangeEvent event = new JavaCodeChangeEvent(text, t);
        bus.post(event);
    }

    public void jslog(String msg) {
        logger.debug(msg);
    }

    public void showExercise(Exercise exercise) {
        String jscallCode = "editor.setValue('" + exercise.getCode().getCode().replaceAll("\\n", "\\\\n") + "')";
        code.getEngine().executeScript(jscallCode);
        codeLabel.setText(exercise.getCode().getName());
        String jscallTest = "editor.setValue('" + exercise.getTest().getCode().replaceAll("\\n", "\\\\n") + "')";
        tests.getEngine().executeScript(jscallTest);
        testLabel.setText(exercise.getTest().getName());
        clearHistory();
        // code.clear();
        // code.appendText();
        // tests.clear();
        // tests.appendText(exercise.getTest().getCode());
        // revertToCode = code.getText();
        // revertToTest = tests.getText();
        // code.selectRange(0, 0);
        // tests.selectRange(0, 0);

    }

    private void clearHistory() {
        code.getEngine().executeScript("clearHistory()");
        tests.getEngine().executeScript("clearHistory()");
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString("");
        clipboard.setContent(content);
    }

    @Subscribe
    private void resetToRed(ResetPhaseEvent event) {
        String jscallCode = "editor.setValue('" + revertToCode.replaceAll("\\n", "\\\\n") + "')";
        code.getEngine().executeScript(jscallCode);
        String jscallTest = "editor.setValue('" + revertToTest.replaceAll("\\n", "\\\\n") + "')";
        tests.getEngine().executeScript(jscallTest);
        clearHistory();
    }

    @Subscribe
    private void changePhaseToRed(SwitchedToRedEvent event) {
        code.getEngine().executeScript("editor.setOption('readOnly', true)");
        tests.getEngine().executeScript("editor.setOption('readOnly', false)");
        revertToTest = getTest();
        revertToCode = getCode();
        clearHistory();
        tests.getEngine().executeScript("activate('crimson','rgba(220,20,60,0.1)')");
        code.getEngine().executeScript("deactivate()");
        iGreenBox.setVisible(false);
        iRedLabel1.setText("Write code to make all tests pass");
        AnchorPane.setRightAnchor(codeBox, 15.0);
    }

    @Subscribe
    private void changePhaseToGreen(SwitchedToGreenEvent event) {
        code.getEngine().executeScript("editor.setOption('readOnly', false)");
        tests.getEngine().executeScript("editor.setOption('readOnly', true)");
        revertToTest = getTest();
        clearHistory();
        code.getEngine().executeScript("activate('forestgreen','rgba(34,139,34,0.1)')");
        tests.getEngine().executeScript("deactivate()");
        iGreenBox.setVisible(true);
        AnchorPane.setRightAnchor(codeBox, iGreenBox.getWidth() + 10);
    }

    @Subscribe
    private void changePhaseToRefactor(SwitchedToRefactorEvent event) {
        code.getEngine().executeScript("editor.setOption('readOnly', false)");
        tests.getEngine().executeScript("editor.setOption('readOnly', false)");
        tests.getEngine().executeScript("activate('orange','#FFC')");
        code.getEngine().executeScript("activate('orange','#FFC')");
        clearHistory();
        iGreenBox.setVisible(true);
        iRedLabel1.setText("Modify code, but keep all tests passing");
        AnchorPane.setRightAnchor(codeBox, 15.0);
    }

    private void addEditors() {
        code = new WebView();
        code.setContextMenuEnabled(false);
        String resource = EditorViewController.class.getResource("/editor.html").toExternalForm();
        code.getEngine().load(resource);

        // code.setEditable(false);
        codePane.getChildren().add(code);
        AnchorPane.setTopAnchor(code, 50.0);
        AnchorPane.setLeftAnchor(code, 20.0);
        AnchorPane.setRightAnchor(code, 20.0);
        AnchorPane.setBottomAnchor(code, 5.0);

        tests = new WebView();
        tests.getEngine().load(resource);
        tests.setContextMenuEnabled(false);
        // tests.setEditable(false);
        testPane.getChildren().add(tests);
        AnchorPane.setTopAnchor(tests, 50.0);
        AnchorPane.setLeftAnchor(tests, 20.0);
        AnchorPane.setRightAnchor(tests, 20.0);
        AnchorPane.setBottomAnchor(tests, 5.0);

        JSObject jsobj = (JSObject) code.getEngine().executeScript("window");
        jsobj.setMember("java", this);
        jsobj.setMember("type", "code");

        jsobj = (JSObject) tests.getEngine().executeScript("window");
        jsobj.setMember("java", this);
        jsobj.setMember("type", "test");

    }

    public void zoomIn() {
        fontSize += 1;
        applyFontSize();
    }

    public void zoomOut() {
        if (fontSize > 8)
            fontSize -= 1;
        applyFontSize();
    }

    public void zoomDefault() {
        fontSize = 13;
        applyFontSize();
    }

    private void applyFontSize() {
        String style = "-fx-font-size:" + fontSize + "px";
        code.getEngine().executeScript("changeFontSize(" + fontSize + ")");
        tests.getEngine().executeScript("changeFontSize(" + fontSize + ")");
    }

    private static vk.core.api.TestResult getTestResult(AutoCompilerResult result) {
        vk.core.api.TestResult results = null;

        try {
            Field field = result.getClass().getDeclaredField("testResult");
            field.setAccessible(true);
            results = (vk.core.api.TestResult) field.get(result);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // ignored;
        }

        return results;
    }

    private void updateTestResultView(vk.core.api.TestResult testResult) {
        testresultview.clear();

        testResult.getTestFailures().forEach(failure ->
                testresultview.add(new TestResult(failure.getTestClassName(), failure.getMethodName(),
                                                  failure.getMessage(), 0, true)));
    }

    @Subscribe
    public void compileResult(AutoCompilerResult result) {
        testOutput.clear();
        testresultview.clear();

        if (result.allClassesCompile()) {
            if (result.allTestsGreen()) {
                status.setText("Code and Test compile, and the tests are passing.");
                status.setStyle("-fx-text-fill: white");
                statuscontainer.setStyle("-fx-background-color: green");
                testOutput.setText(result.getTestOutput());
            } else {
                status.setText("Code and Test compile, but the tests are not passing.");
                status.setStyle("-fx-text-fill: white");
                statuscontainer.setStyle("-fx-background-color: red");

                vk.core.api.TestResult testResult = getTestResult(result);
                updateTestResultView(testResult);
                testOutput.setText(result.getTestOutput());
            }
        } else {
            status.setText("Code or Test (or both) contain errors.");
            status.setStyle("-fx-text-fill: white");
            statuscontainer.setStyle("-fx-background-color: black");
            testOutput.setText(result.getCompilerOutput());
        }
    }

}
