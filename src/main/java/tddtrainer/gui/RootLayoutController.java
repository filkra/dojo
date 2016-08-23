package tddtrainer.gui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import tddtrainer.catalog.Exercise;
import tddtrainer.catalog.JavaClass;
import tddtrainer.events.ExerciseEvent;
import tddtrainer.events.LanguageChangeEvent;
import tddtrainer.events.PhaseChangeEvent;
import tddtrainer.events.PhaseResetEvent;
import tddtrainer.events.TimeEvent;
import tddtrainer.events.TrackingWindowEvent;
import tddtrainer.handbook.Handbook;
import tddtrainer.logic.Phase;
import tddtrainer.logic.PhaseManager;
import tddtrainer.logic.PhaseStatus;

public class RootLayoutController extends BorderPane implements Initializable {

	@FXML
	private BorderPane root;

	@FXML
	private Button resetButton;

	@FXML
	private Button nextStepButton;

	@FXML
	private Label statusLabel;

	@FXML
	private Label exerciseLabel;

	@FXML
	private Label timeLabel;

	@FXML
	private ImageView timerImage;

	@FXML
	private HBox iRedBox;

	@FXML
	private MenuItem showDescription;

	private ResourceBundle resources;

	private PhaseManager phaseManager;

	private EventBus bus;

	@FXML
	private AnchorPane rootPane;

	@FXML
	private EditorViewController editors;

	Logger logger = LoggerFactory.getLogger(RootLayoutController.class);

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;
		enableReset(false);
		enableShowDescription(false);
		hideRedBox();
	}

	@Inject
	public RootLayoutController(FXMLLoader loader, EventBus bus, PhaseManager phaseManager) {
		this.phaseManager = phaseManager;
		this.bus = bus;
		bus.register(this);
		URL resource = getClass().getResource("RootLayout.fxml");
		loader.setLocation(resource);
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("Error loading Root view", e);
		}
	}

	@Subscribe
	public void updateTime(TimeEvent event) {
		long time = event.getTime();
		Platform.runLater(() -> {
			timerImage.setVisible(true);
			timeLabel.setText("" + time);
			if (time <= 5) {
				timeLabel.setFont(new Font("System bold", 18.0));
			} else if (time <= 10) {
				timeLabel.setFont(new Font("System", 15.0));
				timeLabel.setStyle("-fx-text-fill: crimson");
			} else {
				timeLabel.setFont(new Font("System", 15.0));
				timeLabel.setStyle("-fx-text-fill: #6f8391");
			}
		});
	}

	@FXML
	private void changeLanguage(ActionEvent event) {
		List<String> choices = new ArrayList<>();
		choices.add("English");
		choices.add("Deutsch");

		ChoiceDialog<String> dialog = new ChoiceDialog<>("English", choices);
		dialog.setTitle(resources.getString("languagedialog.title"));
		dialog.setHeaderText(resources.getString("languagedialog.headerText"));
		dialog.setContentText(resources.getString("languagedialog.contentText"));

		Optional<String> result = dialog.showAndWait();

		Locale locale = result.isPresent() && result.get().equals("English") ? new Locale("en", "EN")
				: result.isPresent() && result.get().equals("Deutsch") ? new Locale("de", "DE") : resources.getLocale();

		if (!resources.getLocale().toString().equals(locale.toString().substring(0, 2))) {

			this.resources = ResourceBundle.getBundle("bundles.tddt", locale);
			bus.post(new LanguageChangeEvent(resources));
			bus.post(new PhaseResetEvent());
		}

	}

	@Subscribe
	void changePhase(PhaseChangeEvent phaseChangeEvent) {
		Phase phase = phaseChangeEvent.getPhase();
		switch (phase) {
		case RED:
			switchToStatusRed();
			break;
		case GREEN:
			switchToStatusGreen();
			break;
		case REFACTOR:
			switchToStatusRefactor();
			break;
		}
	}

	public void switchToStatusRed() {
		System.out.println("red");
		showRedBox();
		enableReset(true);
		// statusLabel.setText("red");
		// statusLabel.getStyleClass().clear();
		// statusLabel.getStyleClass().add("statuslabel-red");
	}

	public void switchToStatusGreen() {
		System.out.println("green");
		enableReset(true);
		hideRedBox();
		// statusLabel.setText("green");
		// statusLabel.getStyleClass().clear();
		// statusLabel.getStyleClass().add("statuslabel-green");
	}

	public void switchToStatusRefactor() {
		System.out.println("refactor");
		enableReset(false);
		hideRedBox();
		timeLabel.setText("");
		timerImage.setVisible(false);
		// statusLabel.setText("refactor");
		// statusLabel.getStyleClass().clear();
		// statusLabel.getStyleClass().add("statuslabel-refactor");
	}

	@FXML
	private void selectExercise(ActionEvent event) {
		phaseManager.selectExercise();
	}

	@FXML
	private void showProgress(ActionEvent event) {
		bus.post(new TrackingWindowEvent());
	}

	@FXML
	private void reset(ActionEvent event) {
		bus.post(new PhaseResetEvent());
	}

	// @FXML
	// private void handleTutorialMode(ActionEvent event) {
	// CheckMenuItem item = (CheckMenuItem) event.getSource();
	// editorViewController.setTutorialMode(item.isSelected());
	// }

	@FXML
	private void handleNextStep(ActionEvent event) {
		Exercise exercise = newExerciseFromCurrentInput();
		PhaseStatus status = phaseManager.checkPhase(exercise, true);
		bus.post(new PhaseChangeEvent(status.getPhase()));
	}

	private Exercise newExerciseFromCurrentInput() {
		Exercise oldExercise = phaseManager.getOriginalExercise();
		Exercise exercise = new Exercise(oldExercise.getName(), oldExercise.getDescription());
		exercise.addCode(new JavaClass(oldExercise.getCode(0).getName(), editors.getCode()));
		exercise.addTest(new JavaClass(oldExercise.getTest(0).getName(), editors.getTest()));
		return exercise;
	}

	@FXML
	private void showExerciseDescription(ActionEvent event) {
		String description = phaseManager.getOriginalExercise().getDescription();
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(resources.getString("description"));
		alert.setHeaderText(null);
		alert.setContentText(description);
		alert.showAndWait();
	}

	@FXML
	private void showHandbook(ActionEvent event) {
		try {
			new Handbook().showPDF();
		} catch (Exception e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle(resources.getString("handbook.error.title"));
			alert.setHeaderText(null);
			alert.setContentText(resources.getString("handbook.error.message") + "\n" + e.getMessage());
			alert.showAndWait();
		}
	}

	protected void enableReset(boolean enable) {
		resetButton.setDisable(!enable);
	}

	protected void enableShowDescription(boolean enable) {
		showDescription.setDisable(!enable);
	}

	@Subscribe
	public void showExercise(ExerciseEvent exerciseEvent) {
		Exercise exercise = exerciseEvent.getExercise();

		if (exercise != null) {
			exerciseLabel.setText(exercise.getName());
			exerciseLabel.setTooltip(new Tooltip(exercise.getName()));
			enableShowDescription(true);
		}
		nextStepButton.setDisable(false);
	}

	public void hideRedBox() {
		iRedBox.setVisible(false);
	}

	public void showRedBox() {
		iRedBox.setVisible(true);
		iRedBox.toFront();
	}

}
