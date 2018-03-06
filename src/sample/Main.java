package sample;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    public double values[][];
    public double datas=0.0,increment=0.0;
    public String parameter[]=new String[30];
    public int noOfParameter=-1;
    public double range[][];
    public double cValue = 6;

    public Label fileError;
    public Label cValueError;
    public BorderPane layout;


    @Override
    public void start(Stage primaryStage) throws Exception{

        Button loadLas = new Button("Load file");
        loadLas.setOnAction(e->{
            FileChooser loadlasdirrctory = new FileChooser();
            loadlasdirrctory.getExtensionFilters().add(new FileChooser.ExtensionFilter("LAS Files", "*.las"));
            loadlasdirrctory.setTitle("Load LAS file for NMR estimation ");
            File selectedlas =  loadlasdirrctory.showOpenDialog(primaryStage);
            if(selectedlas != null){
                try {
                    loadlas(selectedlas);
                }
                catch (IOException el){
                    el.printStackTrace();
                }
            }
            else
                handleError("file", "file not laoded/ cancelled");
        });
        fileError = new Label("");
        fileError.setFont(new Font("Arial", 11));
        fileError.setStyle("-fx-text-fill: red;");
        VBox fileHB = new VBox(20, loadLas, fileError);

        TextField cValue = new TextField("6");
        cValue.setPromptText("enter c value in range 5-7");
        Button updateCValue = new Button("update 'c' value");
        updateCValue.setOnAction(e->{
            
        });
        cValueError = new Label("");
        cValueError.setFont(new Font("Arial", 11));
        cValueError.setStyle("-fx-text-fill: red;");
        VBox cValueHB = new VBox(20, new HBox(25,cValue, updateCValue), cValueError);

        layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setTop(new HBox(50,loadLas, ));

        primaryStage.setTitle("Permeability estimation using NMR log - Siddharth's Dissertation");
        primaryStage.setScene(new Scene(layout, 600, 400));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public void loadlas(File selectedlas)throws IOException{
        BufferedReader bufferedReader;
        try {
            //BufferedReader fot LAS/ ASCII file input
            bufferedReader = new BufferedReader(new FileReader(selectedlas));

            String text;
            Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)"); //regex used as delimiter
            boolean Isdata = false, Isparameter = false;
            int index = 0, parameterindex = -1;

            //Reading each line and storing each parameter value to temporary matrix
            while ((text = bufferedReader.readLine()) != null && text.length() > 0) {

                //Checking for empty lines
                if (Isdata) {

                    //Replacing multiple line spaces to single space
                    text = (text.replaceAll("[ ]+", " ")).substring(1);
                    text += " ";
                    int textindex = 0;

                    //finding indexOF of spaces which gives me parameter value just before that
                    while (text.indexOf(" ", textindex) > 0) {
                        if (++parameterindex == 0)
                            values[index][parameterindex] = 0.3048 * Double.parseDouble(text.substring(textindex, text.indexOf(" ", textindex)));
                            //0.3048 factor to convert meter to feet
                        else{
                            values[index][parameterindex] = Double.parseDouble(text.substring(textindex, text.indexOf(" ", textindex)));
                            if(index==0){

                                //Storing minimum at index 1 and maximum at index 2.
                                range[0][parameterindex]=values[index][parameterindex];
                                range[1][parameterindex]=values[index][parameterindex];
                            }
                            else {
                                if(range[0][parameterindex]>values[index][parameterindex])
                                    range[0][parameterindex]=values[index][parameterindex];
                                if(range[1][parameterindex]<values[index][parameterindex])
                                    range[1][parameterindex]=values[index][parameterindex];
                            }
                        }
                        textindex = text.indexOf(" ", textindex) + 1;
                    }
                    ++index;
                    parameterindex = -1;

                    //Reinitializing parameterindexIndex = -1 to start for new line
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STRT"))) {

                    //Checking for Start Depth Data and working accordingly
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        datas -= Double.parseDouble(matcher.group(1));
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STOP"))) {

                    //Checking for End Depth Data and working accordingly
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        datas += Double.parseDouble(matcher.group(1));
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STEP"))) {

                    //Checking for Step Increment Data and working accordingly
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        increment = Double.parseDouble(matcher.group(1));
                        datas /= increment;
                    }
                } else if (text.length() > 5 && (text.substring(1, 6).equalsIgnoreCase("depth"))) {

                    //Checking for Start Depth Data and working accordingly
                    Isparameter = true;
                    parameter[++noOfParameter] = "Depth in meter";
                } else if (Isparameter && !(text.substring(0, 4).equalsIgnoreCase("~Asc"))) {

                    //"~Asc" Statement confirms start of Data
                    parameter[++noOfParameter] = text.substring(1, text.indexOf(" ", 1));
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("~Asc"))) {

                    //Once Data is confirmed initialize values matrix for reading las/ASCII file values
                    Isdata = true;
                    values = new double[(int) datas + 1][noOfParameter + 1];
                    range= new double[2][noOfParameter + 1];
                }
            }
            range[0][0] = values[0][0];
            range[1][0] = values[(int) datas][0];

        }
        catch (FileNotFoundException ex) {}
        catch (IOException ex) {}
        finally {}
    }

    public void handleError(String errorLabel, String error){
        if (errorLabel.equals("file"))
            fileError.setText(error);
        else
            cValueError.setText(error);
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        if (errorLabel.equals("file"))
            sleeper.setOnSucceeded(event-> fileError.setText(""));
        else
            sleeper.setOnSucceeded(event-> cValueError.setText(""));

        new Thread(sleeper).start();
    }
}
