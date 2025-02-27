package ca.bradj.horsehotel.gui;

public class GuiUtil {

    private static boolean isCoordInBox(
            double mouseX,
            double mouseY,
            int leftX,
            int topY,
            int width,
            int height
    ) {
        return mouseX >= leftX && mouseY >= topY && mouseX < leftX + width && mouseY < topY + height;
    }


    public static boolean isCoordInBox(
            Coordinate coord,
            Coordinate boxTopLeft,
            Coordinate boxBottomRight
    ) {
        return isCoordInBox(
                coord.x(),
                coord.y(),
                boxTopLeft.x(),
                boxTopLeft.y(),
                boxBottomRight.x() - boxTopLeft.x(),
                boxBottomRight.y() - boxTopLeft.y()
        );
    }

}
