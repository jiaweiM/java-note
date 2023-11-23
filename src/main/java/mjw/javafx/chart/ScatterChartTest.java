package mjw.javafx.chart;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * @author JiaweiMao
 * @version 0.0.1
 * @since 20 4月 2022, 10:29
 */
public class ScatterChartTest extends Application {

    public static void main(String[] args) {

        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Year");
// Customize the x-axis, so points are scattered uniformly
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(1900);
        xAxis.setUpperBound(2300);
        xAxis.setTickUnit(50);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Population (in millions)");
        ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);
        chart.setTitle("Population by Year and Country");
// Set the data for the chart
        ObservableList<XYChart.Series<Number, Number>> chartData =
                XYChartDataUtil.getCountrySeries();
        chart.setData(chartData);
        StackPane root = new StackPane(chart);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("A Scatter Chart");
        stage.show();
    }
}
