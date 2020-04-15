package wam;

import wam.view.GridPosition;

import java.io.IOException;

public class Responder {
    private ResponseCSVEncoder encoder;
    private Communication communication;

    public Responder(ResponseCSVEncoder encoder, Communication communication) {
        this.encoder = encoder;
        this.communication = communication;
    }

    public void respond(long round, GridPosition clickedCell) throws IOException {
        String csv = encoder.toCSV(round, clickedCell);
        communication.send(csv);
    }

}
