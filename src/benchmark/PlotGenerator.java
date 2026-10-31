package benchmark;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PlotGenerator {
    private static final Color[] COLORS = {
            new Color(37, 99, 235),
            new Color(220, 38, 38),
            new Color(5, 150, 105)
    };

    private record Point(
            String algorithm,
            String dataset,
            int vertices,
            int edges,
            double milliseconds
    ) {
    }

    private record Series(String algorithm, String dataset, String label) {
    }

    private PlotGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path tables = Path.of("results", "tables", "averages.csv");
        Path plots = Path.of("results", "plots");
        Files.createDirectories(plots);

        List<Point> points = readPoints(tables);
        if (points.size() != 27) {
            throw new IllegalArgumentException(
                    "Expected 27 average rows, found " + points.size()
            );
        }

        createPlot(points, true, plots.resolve("time-vs-vertices.png"));
        createPlot(points, false, plots.resolve("time-vs-edges.png"));

        System.out.println("Both plots created.");
        System.out.println("Plots directory: " + plots.toAbsolutePath());
    }

    private static List<Point> readPoints(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty averages file");
        }

        List<String> header = List.of(lines.get(0).split(",", -1));
        int algorithm = column(header, "algorithm");
        int dataset = column(header, "dataset");
        int vertices = column(header, "vertices");
        int edges = column(header, "edges");
        int time = column(header, "average_time_ms");

        List<Point> points = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] cells = lines.get(i).split(",", -1);
            double milliseconds = Double.parseDouble(cells[time]);
            if (!Double.isFinite(milliseconds) || milliseconds < 0) {
                throw new IllegalArgumentException("Invalid execution time");
            }

            points.add(new Point(
                    cells[algorithm],
                    cells[dataset],
                    Integer.parseInt(cells[vertices]),
                    Integer.parseInt(cells[edges]),
                    milliseconds
            ));
        }

        return points;
    }

    private static int column(List<String> header, String name) {
        int index = header.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Missing column: " + name);
        }
        return index;
    }

    private static void createPlot(
            List<Point> points, boolean byVertices, Path output
    ) throws IOException {
        BufferedImage image = new BufferedImage(
                1600, 1100, BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            graphics.setColor(new Color(25, 35, 50));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            graphics.drawString(
                    byVertices
                            ? "Execution time vs. number of vertices"
                            : "Execution time vs. number of edges",
                    40, 42
            );
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            graphics.drawString(
                    "Mean of 5 measured runs after 20 warmups; seed 42",
                    40, 69
            );

            drawPanel(graphics, points, byVertices, 20, 85,
                    "Minimum spanning tree",
                    List.of(
                            new Series("Kruskal", "undirected", "Kruskal"),
                            new Series("Prim", "undirected", "Prim")
                    ));

            drawPanel(graphics, points, byVertices, 810, 85,
                    "Topological sorting",
                    List.of(
                            new Series("TopologicalSort", "dag", "DAG"),
                            new Series("TopologicalSort", "cyclic", "Cyclic")
                    ));

            drawPanel(graphics, points, byVertices, 20, 565,
                    "Strongly connected components: Kosaraju",
                    List.of(
                            new Series("Kosaraju", "dag", "DAG"),
                            new Series("Kosaraju", "cyclic", "Cyclic")
                    ));

            drawPanel(graphics, points, byVertices, 810, 565,
                    "Shortest paths: Dijkstra, source 0",
                    List.of(
                            new Series("Dijkstra", "undirected", "Undirected"),
                            new Series("Dijkstra", "dag", "DAG"),
                            new Series("Dijkstra", "cyclic", "Cyclic")
                    ));

            graphics.setColor(Color.DARK_GRAY);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            graphics.drawString(
                    "X axes are logarithmic; Y axes are linear and scaled "
                            + "separately. Lines connect measured configurations.",
                    40, 1060
            );
            graphics.drawString(
                    "V and E change together across configurations; "
                            + "these plots do not isolate their individual effects.",
                    40, 1085
            );
        } finally {
            graphics.dispose();
        }

        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("PNG writer unavailable");
        }
    }

    private static void drawPanel(
            Graphics2D graphics,
            List<Point> points,
            boolean byVertices,
            int x,
            int y,
            String title,
            List<Series> series
    ) {
        int left = x + 95;
        int right = x + 735;
        int top = y + 65;
        int bottom = y + 340;

        double minimumX = byVertices ? 10 : 30;
        double maximumX = byVertices ? 1000 : 5000;
        double maximumY = 0;

        List<List<Point>> groups = new ArrayList<>();

        for (Series item : series) {
            List<Point> group = points.stream()
                    .filter(point -> point.algorithm().equals(item.algorithm())
                            && point.dataset().equals(item.dataset()))
                    .sorted(Comparator.comparingInt(Point::vertices))
                    .toList();

            if (group.size() != 3) {
                throw new IllegalArgumentException(
                        "Expected three sizes for " + item
                );
            }

            groups.add(group);
            for (Point point : group) {
                maximumY = Math.max(maximumY, point.milliseconds());
            }
        }

        maximumY = maximumY > 0 ? maximumY * 1.15 : 0.001;

        graphics.setColor(new Color(25, 35, 50));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        graphics.drawString(title, x + 25, y + 27);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        graphics.drawString("Average time (ms)", left, top - 15);

        graphics.setStroke(new BasicStroke(1));

        for (int tick = 0; tick <= 5; tick++) {
            double value = maximumY * tick / 5.0;
            int position = bottom - (bottom - top) * tick / 5;

            graphics.setColor(new Color(225, 230, 235));
            graphics.drawLine(left, position, right, position);
            graphics.setColor(Color.DARK_GRAY);

            String label = String.format(Locale.ROOT, "%.3g", value);
            graphics.drawString(
                    label,
                    left - 12 - graphics.getFontMetrics().stringWidth(label),
                    position + 5
            );
        }

        int[] ticks = byVertices
                ? new int[]{10, 100, 1000}
                : new int[]{30, 500, 5000};

        for (int value : ticks) {
            int position = xPosition(value, minimumX, maximumX, left, right);
            graphics.setColor(new Color(225, 230, 235));
            graphics.drawLine(position, top, position, bottom);
            graphics.setColor(Color.DARK_GRAY);

            String label = Integer.toString(value);
            graphics.drawString(
                    label,
                    position - graphics.getFontMetrics().stringWidth(label) / 2,
                    bottom + 24
            );
        }

        graphics.setColor(Color.DARK_GRAY);
        graphics.drawLine(left, top, left, bottom);
        graphics.drawLine(left, bottom, right, bottom);
        graphics.drawString(
                byVertices ? "Vertices (log scale)" : "Edges (log scale)",
                left + 240, bottom + 48
        );

        for (int index = 0; index < groups.size(); index++) {
            graphics.setColor(COLORS[index]);
            graphics.setStroke(new BasicStroke(2.5f));

            int previousX = 0;
            int previousY = 0;
            boolean first = true;

            for (Point point : groups.get(index)) {
                int pointX = xPosition(
                        byVertices ? point.vertices() : point.edges(),
                        minimumX, maximumX, left, right
                );
                int pointY = bottom - (int) Math.round(
                        point.milliseconds() / maximumY * (bottom - top)
                );

                if (!first) {
                    graphics.drawLine(previousX, previousY, pointX, pointY);
                }

                graphics.fillOval(pointX - 5, pointY - 5, 10, 10);
                previousX = pointX;
                previousY = pointY;
                first = false;
            }

            int legendY = bottom + 75 + index * 22;
            graphics.drawLine(left, legendY - 5, left + 28, legendY - 5);
            graphics.drawString(series.get(index).label(), left + 40, legendY);
        }
    }

    private static int xPosition(
            double value,
            double minimum,
            double maximum,
            int left,
            int right
    ) {
        double fraction = (Math.log10(value) - Math.log10(minimum))
                / (Math.log10(maximum) - Math.log10(minimum));

        return left + (int) Math.round(fraction * (right - left));
    }
}