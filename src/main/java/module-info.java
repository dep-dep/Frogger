module ca.ucalgary.cpsc.frogger {
    requires javafx.controls;
    requires javafx.fxml;


    opens ca.ucalgary.cpsc.frogger to javafx.fxml;
    exports ca.ucalgary.cpsc.frogger;
}