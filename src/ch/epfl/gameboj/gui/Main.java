package ch.epfl.gameboj.gui;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.gui.ImageConverter;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdController;
import javafx.application.*;
import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

import javafx.stage.Stage;

public final class Main extends Application {
	
	private long start;
	private GameBoy gb;
	private javafx.scene.image.Image image;
	private static final Map<String, Joypad.Key> KEYS = Map.of(KeyCode.A.getChar(), Joypad.Key.A, KeyCode.B.getChar(),
			Joypad.Key.B, KeyCode.SPACE.getChar(), Joypad.Key.SELECT, KeyCode.S.getChar(), Joypad.Key.START);
	private static final Map<KeyCode, Joypad.Key> DIRECTION = Map.of(KeyCode.RIGHT, Joypad.Key.RIGHT, KeyCode.LEFT,
			Joypad.Key.LEFT, KeyCode.UP, Joypad.Key.UP, KeyCode.DOWN, Joypad.Key.DOWN);

	/**
	 * Lance le démarrage de l'application javaFx
	 * 
	 * @param args
	 *            : doit contenir une seule chaine représentant le fichier Gamboy
	 *            auquel on veut jouer. Doit être non null.
	 * 
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 * 
	 * @Throws IllegalArgumentException si args n'est pas de longueur 1 et ne
	 * contient pas une chaine qui si termine par ".gb"
	 */
	@Override
	public void start(Stage arg0) throws Exception {

		// Validation des arguments et création de Gamboy
		List<String> parameters = getParameters().getRaw();
		Preconditions.checkArgument(parameters.size() == 1);
		Objects.requireNonNull(parameters.get(0));
		Preconditions.checkArgument(parameters.get(0).endsWith(".gb"));
		if (parameters.size() != 1 || parameters.get(0) == null) {
			System.exit(1);
		}
		File romFile = new File(parameters.get(0));
		gb = new GameBoy(Cartridge.ofFile(romFile));

		// Création de l'interface JavaFx
		ImageView imageV = new ImageView();
		imageV.setFitWidth(LcdController.LCD_WIDTH * 2);
		imageV.setFitHeight(LcdController.LCD_HEIGHT * 2);
		BorderPane root = new BorderPane(imageV);
		Scene scene = new Scene(root);
		arg0.setScene(scene);
		imageV.fitWidthProperty().bind(root.widthProperty());
		imageV.fitHeightProperty().bind(root.heightProperty());
		arg0.sizeToScene();
		arg0.minWidthProperty().bind(scene.heightProperty());
		arg0.minHeightProperty().bind(scene.widthProperty());
		arg0.setTitle("GameBoy");
		arg0.show();
		imageV.requestFocus();

		// Gestion des controles
		imageV.setOnKeyPressed((event) -> {
			Joypad.Key key = DIRECTION.getOrDefault(event.getCode(), KEYS.get(event.getText().toUpperCase()));
			if (key != null)
				gb.joypad().keyPressed(key);
		});
		imageV.setOnKeyReleased((event) -> {
			Joypad.Key key = DIRECTION.getOrDefault(event.getCode(), KEYS.get(event.getText().toUpperCase()));
			if (key != null)
				gb.joypad().keyReleased(key);
		});

		// Timer
		start = System.nanoTime();
		AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				long elapsed = (now - start);
				gb.runUntil((long) (elapsed * GameBoy.CYCLES_PER_NANOSECOND));
				image = ImageConverter.convert(gb.lcdController().currentImage());
				imageV.setImage(image);
			}
		};
		timer.start();

	}
}
