import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayDeque;
import javalib.impworld.*;
import javalib.worldimages.*;
import tester.Tester;


/*----------------------------------------------------------------*/
/*
 * HOW TO PLAY:
 * Press b for a breadth-first algorithm
 * Press d for a depth-first algorithm
 * Press m to attempt to solve the maze manually
 *  Use the arrow keys to traverse the maze, the red square is your
 *  current position
 *  Once completed the way the computer would have solved it is shown
 *  and then the correct path is highlighted
 *
 * When using either a breadth-first or depth-first algorithm the correct
 * path will be highlighted immediately after completion
 *
 * At any time you can press r to generate a new random maze and then
 * press either b d or m
 *
 */
/*----------------------------------------------------------------*/

// represents a node of the graph which represents a cell of the maze
class Cell {
  Edge left;
  Edge right;
  Edge top;
  Edge bottom;

  Posn coord;

  Cell(Posn coord) {
    this.coord = coord;
    this.left = null;
    this.right = null;
    this.top = null;
    this.bottom = null;
  }

  // EFFECT: set the left edge to the given edge
  void updateLeft(Edge e) {
    this.left = e;
  }

  // EFFECT: set the right edge to the given edge
  void updateRight(Edge e) {
    this.right = e;
  }

  // EFFECT: set the top edge to the given edge
  void updateTop(Edge e) {
    this.top = e;
  }

  // EFFECT: set the bottom edge to the given edge
  void updateBottom(Edge e) {
    this.bottom = e;
  }

  // if you draw a line between this cell and that cell, is it horizontal?
  boolean horizontalBetween(Cell that) {
    return this.coord.y == that.coord.y;
  }

  // if you draw a line between this cell and that cell, is it vertical?
  boolean verticalBetween(Cell c) {
    return this.coord.x == c.coord.x;
  }

  // place all of this cell's edges onto the WorldScene at the correct coordinates
  WorldScene drawCell(WorldScene s, int size) {
    if (this.left == null) {
      s.placeImageXY(
          new LineImage(new Posn(0, size), Color.BLACK),
          (this.coord.x * size),
          (this.coord.y * size) + size / 2);
    }
    if (this.right == null) {
      s.placeImageXY(
          new LineImage(new Posn(0, size), Color.BLACK),
          (this.coord.x * size) + size,
          (this.coord.y * size) + size / 2);
    }
    if (this.top == null) {
      s.placeImageXY(
          new LineImage(new Posn(size, 0), Color.BLACK),
          (this.coord.x * size) + size / 2,
          (this.coord.y * size));
    }
    if (this.bottom == null) {
      s.placeImageXY(
          new LineImage(new Posn(size, 0), Color.BLACK),
          (this.coord.x * size) + size / 2,
          (this.coord.y * size) + size);
    }

    return s;
  }

  //EFFECT: a helper method for the search method, adds this Cell's neighbors to the worklist
  void addNeighbors(Deque<Cell> worklist, ArrayList<Cell> alreadySeen,
                    HashMap<Cell, Cell> cameFromEdge, boolean dfs) {
    if (this.right != null && !alreadySeen.contains(this.right.cell2)) {
      if (dfs) {
        worklist.addFirst(this.right.cell2);
      }
      else {
        worklist.addLast(this.right.cell2);
      }
      cameFromEdge.put(this.right.cell2, this);
    }
    if (this.bottom != null && !alreadySeen.contains(this.bottom.cell2)) {
      if (dfs) {
        worklist.addFirst(this.bottom.cell2);
      }
      else {
        worklist.addLast(this.bottom.cell2);
      }
      cameFromEdge.put(this.bottom.cell2, this);
    }
    if (this.left != null && !alreadySeen.contains(this.left.cell1)) {
      if (dfs) {
        worklist.addFirst(this.left.cell1);
      }
      else {
        worklist.addLast(this.left.cell1);
      }
      cameFromEdge.put(this.left.cell1, this);
    }
    if (this.top != null && !alreadySeen.contains(this.top.cell1)) {
      if (dfs) {
        worklist.addFirst(this.top.cell1);
      }
      else {
        worklist.addLast(this.top.cell1);
      }
      cameFromEdge.put(this.top.cell1, this);
    }
  }

}

// represents an edge of a graph, connects 2 cells together
class Edge {
  Cell cell1;
  Cell cell2;
  int weight;

  Edge(Cell cell1, Cell cell2, int weight) {
    this.cell1 = cell1;
    this.cell2 = cell2;
    this.weight = weight;
  }

  // EFFECT: remove this edge from its corresponding cells
  void removeEdge() {
    if (cell1.verticalBetween(cell2)) {
      cell1.updateBottom(null);
      cell2.updateTop(null);
    }
    else if (cell1.horizontalBetween(cell2)) {
      cell1.updateRight(null);
      cell2.updateLeft(null);
    }
  }
}

// a maze, generated using Kruskal's algorithm. also the world state for our program
class Maze extends World {
  static final int PIXEL_WIDTH = 900;
  static final int PIXEL_HEIGHT = 600;

  int width;
  int height;
  int cellSize;
  int currentIndex;
  Random rand;
  ArrayList<Cell> cells;
  ArrayList<Edge> edges;
  ArrayList<Cell> path;
  ArrayList<Cell> cellsToDraw;
  ArrayList<Cell> correctPath;
  ArrayList<Cell> correctCellsToDraw;

  boolean drawing;
  boolean solvingManually;
  ArrayList<Cell> alreadySeen;


  // main maze constructor
  Maze(int width, int height) {
    this.width = width;
    this.height = height;
    this.rand = new Random();
    this.cells = new ArrayList<Cell>();
    this.edges = new ArrayList<Edge>();
    this.cellSize = Math.min(this.PIXEL_WIDTH / width, this.PIXEL_HEIGHT / height);
    if (this.cellSize % 2 == 1) {
      this.cellSize = this.cellSize - 1;
    }
    this.cellsToDraw = new ArrayList<Cell>();
    this.correctCellsToDraw = new ArrayList<Cell>();
    this.path = new ArrayList<Cell>();
    this.correctPath = new ArrayList<Cell>();
    this.drawing = false;
    this.solvingManually = false;
    this.currentIndex = 0;

    this.alreadySeen = new ArrayList<Cell>();
    this.createCells();
    this.connectCellsX();
    this.connectCellsY();
    this.createMaze();
  }

  // convenience constructor
  Maze(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand;
    this.cells = new ArrayList<Cell>();
    this.edges = new ArrayList<Edge>();
    this.cellSize = Math.min(this.PIXEL_WIDTH / width, this.PIXEL_HEIGHT / height);
    if (this.cellSize % 2 == 1) {
      this.cellSize = this.cellSize - 1;
    }
    this.cellsToDraw = new ArrayList<Cell>();
    this.correctCellsToDraw = new ArrayList<Cell>();
    this.path = new ArrayList<Cell>();
    this.correctPath = new ArrayList<Cell>();
    this.drawing = false;
    this.solvingManually = false;
    this.currentIndex = 0;

    this.alreadySeen = new ArrayList<Cell>();
    this.createCells();
    this.connectCellsX();
    this.connectCellsY();
    this.createMaze();
  }

  // convenience constructor 2
  Maze(int width, int height, boolean init, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand;
    this.cells = new ArrayList<Cell>();
    this.edges = new ArrayList<Edge>();
    this.cellSize = Math.min(this.PIXEL_WIDTH / width, this.PIXEL_HEIGHT / height);
    this.solvingManually = false;
    this.currentIndex = 0;
    this.alreadySeen = new ArrayList<Cell>();
    if (this.cellSize % 2 == 1) {
      this.cellSize = this.cellSize - 1;
    }
    if (init) {
      this.createCells();
      this.connectCellsX();
      this.connectCellsY();
      this.createMaze();
    }
  }

  // EFFECT: constructs a random maze by using Kruskal's algorithm and Union/Find
  // to create a minimum spanning tree
  void createMaze() {
    UnionFind uf = new UnionFind(new HashMap<Cell, Cell>());
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    Collections.shuffle(this.edges, this.rand);
    ArrayList<Edge> worklist = new ArrayList<Edge>(this.edges);

    // initialize every node's representative to itself
    uf.initRepresentatives(this.cells);

    // while there's more than one tree
    while (edgesInTree.size() < this.cells.size() - 1) {
      // pick the next edge in the graph
      Edge e = worklist.get(0);
      Cell c1 = e.cell1;
      Cell c2 = e.cell2;

      if (uf.find(c1).equals(uf.find(c2))) {
        // do nothing if a cycle would be created
      }
      else {
        edgesInTree.add(e);
        //e.removeEdge();
        uf.union(uf.find(c1), uf.find(c2));
      }
      worklist.remove(0);
    }

    for (Edge e : this.edges) {
      if (!edgesInTree.contains(e)) {
        e.removeEdge();
      }
    }
  }

  //solve the maze using either BFS or DFS
  HashMap<Cell, Cell> search(boolean dfs) {
    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
    Deque<Cell> worklist = new ArrayDeque<Cell>(); // a Queue for BFS, or a Stack for DFS
    ArrayList<Cell> alreadySeen = new ArrayList<Cell>();

    Cell start = this.cells.get(0);
    Cell target = this.cells.get(this.width * this.height - 1);

    // Initialize the worklist with the start Cell
    worklist.add(start);
    // As long as the worklist isn't empty...
    while (!worklist.isEmpty()) {
      Cell next = worklist.remove();
      if (alreadySeen.contains(next)) {
        // discard it
      }
      else if (next == target) {
        alreadySeen.add(next);
        this.path = alreadySeen;
        this.correctPath.add(target);
        this.reconstruct(cameFromEdge, next);
        return cameFromEdge;
      }
      else {
        alreadySeen.add(next);
        next.addNeighbors(worklist, alreadySeen, cameFromEdge, dfs);
      }
    }
    return cameFromEdge;
  }

  void reconstruct(HashMap<Cell, Cell> finished, Cell last) {
    if (finished.get(last).equals(this.cells.get(0))) {
      this.correctPath.add(finished.get(last));
    }
    else {
      this.correctPath.add(finished.get(last));
      this.reconstruct(finished, finished.get(last));
    }
  }

  // EFFECT: create an initial grid of cells
  void createCells() {
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        Cell toAdd = new Cell(new Posn(x, y));
        this.cells.add(toAdd);
      }
    }
  }

  // EFFECT: create the horizontal edges between every cell in the grid
  void connectCellsX() {
    for (int i = 0; i < this.cells.size(); i++) {
      if ((i + 1) % this.width != 0) {
        Cell c = this.cells.get(i);
        Cell cRight = this.cells.get(i + 1);
        Edge connection = new Edge(c, cRight, 0);
        c.updateRight(connection);
        cRight.updateLeft(connection);
        this.edges.add(connection);
      }
    }
  }

  // EFFECT: create the vertical edges between every cell in the grid
  void connectCellsY() {
    for (int i = 0; i < this.cells.size() - this.width; i++) {
      Cell c = this.cells.get(i);
      Cell cBottom = this.cells.get(i + this.width);
      Edge connection = new Edge(c, cBottom, 0);
      c.updateBottom(connection);
      cBottom.updateTop(connection);
      this.edges.add(connection);
    }
  }


  //The onTick function, draws the board as necessary
  public void onTick() {
    if (this.path.size() > 0) {
      this.cellsToDraw.add(this.path.get(0));
      this.path.remove(0);
    }
    else if (this.correctPath.size() > 0) {
      this.correctCellsToDraw.add(this.correctPath.get(0));
      this.correctPath.remove(0);
    }
    else if (this.alreadySeen.size() > 0 && this.currentIndex != this.width * this.height - 1) {
      this.cellsToDraw = this.alreadySeen;

    } else {
      this.drawing = false;
    }
  }

  //Handles key events, either r d b m or the arrow keys
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.cells = new ArrayList<Cell>();
      this.edges = new ArrayList<Edge>();
      this.cellsToDraw = new ArrayList<Cell>();
      this.path = new ArrayList<Cell>();
      this.drawing = false;
      this.correctCellsToDraw = new ArrayList<Cell>();
      this.correctPath = new ArrayList<Cell>();
      this.drawing = false;
      this.solvingManually = false;
      this.currentIndex = 0;
      this.alreadySeen = new ArrayList<Cell>();
      this.createCells();
      this.connectCellsX();
      this.connectCellsY();
      this.createMaze();
    }

    if (!this.drawing) {
      if (key.equals("d") && !this.solvingManually) {
        this.drawing = true;
        this.search(true);
      }
      else if (key.equals("b") && !this.solvingManually) {
        this.drawing = true;
        this.search(false);
      }
      else if (key.equals("m")) {
        this.solvingManually = true;
        this.drawing = true;
      }
    }
    if (this.solvingManually) {
      if (key.equals("right")) {
        this.maybeMove(1);
      } else if (key.equals("down")) {
        this.maybeMove(2);
      } else if (key.equals("left")) {
        this.maybeMove(3);
      } else if (key.equals("up")) {
        this.maybeMove(4);
      }
    }
  }

  //Update the current index if possible
  void maybeMove(int dir) {
    Cell target = this.cells.get(this.width * this.height - 1);

    if (dir == 1 && this.cells.get(this.currentIndex).right != null) {
      this.currentIndex += 1;
      Cell next = this.cells.get(this.currentIndex);

      if (this.alreadySeen.contains(next)) {
        //Do nothing
      }
      else if (next == target) {
        this.alreadySeen.add(next);

        this.reconstruct(this.search(true), target);
      } else {
        this.alreadySeen.add(next);

      }
    }

    else if (dir == 2 && this.cells.get(this.currentIndex).bottom != null) {
      this.currentIndex += this.width;
      Cell next = this.cells.get(this.currentIndex);

      if (this.alreadySeen.contains(next)) {
        //Do nothing
      }
      else if (next == target) {
        this.alreadySeen.add(next);


        this.reconstruct(this.search(true), target);
      } else {
        this.alreadySeen.add(next);

      }
    }

    else if (dir == 3 && this.cells.get(this.currentIndex).left != null) {
      this.currentIndex -= 1;
      Cell next = this.cells.get(this.currentIndex);

      if (this.alreadySeen.contains(next)) {
        //Do nothing
      }
      else if (next == target) {
        this.alreadySeen.add(next);
        this.reconstruct(this.search(true), next);
      } else {
        this.alreadySeen.add(next);

      }
    }

    else if (dir == 4 && this.cells.get(this.currentIndex).top != null) {
      this.currentIndex -= this.width;
      Cell next = this.cells.get(this.currentIndex);

      if (this.alreadySeen.contains(next)) {
        //Do nothing
      }
      else if (next == target) {
        this.alreadySeen.add(next);
        this.reconstruct(this.search(true), next);
      } else {
        this.alreadySeen.add(next);

      }
    }
  }

  // draw the whole maze
  @Override
  public WorldScene makeScene() {
    WorldScene s = new WorldScene(this.PIXEL_WIDTH, this.PIXEL_HEIGHT);
    WorldImage hehe;

    s = this.drawBorderAndBackground(s);
    s = this.drawStartAndEnd(s);

    for (int i = 0; i < this.cellsToDraw.size(); i++) {
      Cell c = this.cellsToDraw.get(i);
      if (this.solvingManually && c == this.cells.get(this.currentIndex)
          && c != this.cells.get(0)) {
        hehe = new RectangleImage(this.cellSize, this.cellSize, "solid", Color.RED);
      } else if (c != this.cells.get(0)) {
        hehe = new RectangleImage(this.cellSize, this.cellSize, "solid", new Color(102, 178, 255));
      } else {
        hehe = new RectangleImage(this.cellSize, this.cellSize, "solid", new Color(102, 204, 0));
      }
      s.placeImageXY(hehe, this.cellSize * c.coord.x + this.cellSize / 2,
          this.cellSize * c.coord.y + this.cellSize / 2);
    }

    for (int i = 0; i < this.correctCellsToDraw.size(); i++) {
      Cell c1 = this.correctCellsToDraw.get(i);
      WorldImage cell =
          new RectangleImage(this.cellSize, this.cellSize, "solid", new Color(0, 102, 204));
      s.placeImageXY(cell, this.cellSize * c1.coord.x + this.cellSize / 2,
          this.cellSize * c1.coord.y + this.cellSize / 2);

      if (c1.equals(this.cells.get(0))) {
        WorldImage done = new TextImage("The maze is solved.", 30, Color.BLACK);
        s.placeImageXY(done, 175, 550);
      }
    }

    for (Cell c : this.cells) {
      s = c.drawCell(s, this.cellSize);
    }

    return s;
  }

  // draw the background and border of the maze
  WorldScene drawBorderAndBackground(WorldScene s) {
    WorldImage border = new RectangleImage(
        this.width * this.cellSize,
        this.height * this.cellSize,
        "outline",
        Color.BLACK);
    WorldImage bg = new RectangleImage(
        this.width * this.cellSize,
        this.height * this.cellSize,
        "solid",
        new Color(192, 192, 192));

    s.placeImageXY(
        bg, (this.width * this.cellSize) / 2, (this.height * this.cellSize) / 2);
    s.placeImageXY(border, (this.width * this.cellSize) / 2, (this.height * this.cellSize) / 2);


    return s;
  }

  // color the top left corner green and the bottom right corner purple
  WorldScene drawStartAndEnd(WorldScene s) {
    WorldImage topLeft =
        new RectangleImage(this.cellSize - 1, this.cellSize - 1, "solid", new Color(102, 204, 0));
    WorldImage bottomRight =
        new RectangleImage(this.cellSize - 1, this.cellSize - 1, "solid", new Color(153, 0, 153));

    s.placeImageXY(topLeft, this.cellSize / 2 + 1, this.cellSize / 2 + 1);
    s.placeImageXY(
        bottomRight,
        this.width * this.cellSize - this.cellSize / 2 + 1,
        this.height * this.cellSize - this.cellSize / 2 + 1);

    return s;
  }

  public static void main(String[] args) {
    ExamplesMaze m = new ExamplesMaze();
    m.testBigBang(new Tester());
  }
}

// a Union/Find data structure based on a HashMap of both nodes and representatives of type Cell
class UnionFind {
  HashMap<Cell, Cell> representatives;

  UnionFind(HashMap<Cell, Cell> representatives) {
    this.representatives = representatives;
  }

  // initialize every node's representative to itself
  void initRepresentatives(ArrayList<Cell> cells) {
    for (Cell c : cells) {
      representatives.put(c, c);
    }
  }

  // set c1's representative to c2's representative
  void union(Cell c1, Cell c2) {
    this.representatives.put(c1, find(c2));
  }

  // find the given cell's representative recursively
  Cell find(Cell c) {
    if (this.representatives.get(c).equals(c)) {
      return c;
    }
    else {
      return find(this.representatives.get(c));
    }
  }
}

//examples mazes and tests for all methods
class ExamplesMaze {
  Maze maze1;
  Maze maze2;
  Maze maze10;
  Maze maze11;
  Maze maze12;
  Maze maze13;

  Maze m1;
  Maze m2;
  Maze m3;
  Maze m4;
  Maze m5;
  Maze m6;

  HashMap<Cell, Cell> hm;
  UnionFind uf;

  Cell cell1;
  Cell cell2;
  Cell cell3;
  Cell cell4;
  Cell cell5;
  Cell cell55;
  Cell cell6;
  Cell cell66;

  Edge edge1;
  Edge edge2;
  Edge edge3;
  Edge edge4;

  void initData() {
    this.cell1 = new Cell(new Posn(1, 2));
    this.cell2 = new Cell(new Posn(10, 2));
    this.cell3 = new Cell(new Posn(5, 6));
    this.cell4 = new Cell(new Posn(5, 7));
    this.cell5 = new Cell(new Posn(2, 4));
    this.cell55 = new Cell(new Posn(3,4));
    this.cell6 = new Cell(new Posn(7, 7));
    this.cell66 = new Cell(new Posn(5,2));

    this.edge1 = new Edge(this.cell1, this.cell2, 0);
    this.edge2 = new Edge(this.cell3, this.cell4, 0);
    this.edge3 = new Edge(this.cell5, this.cell6, 0);

    this.maze1 = new Maze(100, 60);
    this.maze2 = new Maze(5,5);
    this.maze10 = new Maze(3, 3, new Random(0));
    this.maze11 = new Maze(4, 4, new Random(0));
    this.maze12 = new Maze(5, 2, new Random(0));
    this.maze13 = new Maze(3, 3, false, new Random(0));

    this.m1 = new Maze(3, 3, false, new Random(1));
    this.m2 = new Maze(2,3, false, new Random(2));
    this.m3 = new Maze(3,3, false, new Random(3));
    this.m4 = new Maze(2,2, false, new Random(4));

    this.m5 = new Maze(2,2, new Random(4));
    this.m6 = new Maze(3, 3, new Random(1));
    this.hm = new HashMap<Cell, Cell>();
    this.uf = new UnionFind(hm);
  }

  // showing the maze
  void testBigBang(Tester t) {
    this.initData();
    int width = 900;
    int height = 600;
    // make it run as fast as possible, change for grading if needed
    double tickRate = 0.0000000001;
    this.maze1.bigBang(width, height, tickRate);
  }

  // TESTS FOR THE MAZE CLASS

  void testOnKeyEvent(Tester t) {
    this.initData();

    //Initial conditions
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());

    t.checkExpect(this.maze11.cellsToDraw, new ArrayList<Cell>());
    t.checkExpect(this.maze11.path, new ArrayList<Cell>());
    t.checkExpect(this.maze11.drawing, false);
    t.checkExpect(this.maze11.solvingManually, false);
    t.checkExpect(this.maze11.currentIndex, 0);

    //Modify
    this.maze11.onKeyEvent("r");

    //Post conditions
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());
    t.checkExpect(this.maze11.cellsToDraw, new ArrayList<Cell>());
    t.checkExpect(this.maze11.path, new ArrayList<Cell>());
    t.checkExpect(this.maze11.drawing, false);
    t.checkExpect(this.maze11.solvingManually, false);
    t.checkExpect(this.maze11.currentIndex, 0);

    //Modify
    this.maze11.onKeyEvent("d");

    //Post conditions
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());
    t.checkExpect(this.maze11.cellsToDraw, new ArrayList<Cell>());
    t.checkExpect(this.maze11.drawing, true);
    t.checkExpect(this.maze11.solvingManually, false);
    t.checkExpect(this.maze11.currentIndex, 0);

    //Modify
    this.maze11.onKeyEvent("r");
    this.maze11.onKeyEvent("b");

    //Post conditions
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());
    t.checkExpect(this.maze11.cellsToDraw, new ArrayList<Cell>());
    t.checkExpect(this.maze11.drawing, true);
    t.checkExpect(this.maze11.solvingManually, false);
    t.checkExpect(this.maze11.currentIndex, 0);

    //Modify
    this.maze11.onKeyEvent("m");

    //Post conditions
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());
    t.checkExpect(this.maze11.cellsToDraw, new ArrayList<Cell>());
    t.checkExpect(this.maze11.drawing, true);
    t.checkExpect(this.maze11.solvingManually, false);
    t.checkExpect(this.maze11.currentIndex, 0);

  }

  //Tests for the maybeMove method
  void testMaybeMove(Tester t) {
    this.initData();

    //Initial conditions
    t.checkExpect(this.maze11.currentIndex, 0);
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());

    //Modify
    this.maze11.maybeMove(1);

    //Check post conditions
    t.checkExpect(this.maze11.currentIndex, 0);
    t.checkExpect(this.maze11.alreadySeen, new ArrayList<Cell>());

    this.maze11.maybeMove(2);

    t.checkExpect(this.maze11.currentIndex, 4);
    t.checkExpect(this.maze11.alreadySeen,
        new ArrayList<Cell>(Arrays.asList(this.maze11.cells.get(4))));

    this.maze11.maybeMove(2);

    t.checkExpect(this.maze11.currentIndex, 8);
    t.checkExpect(this.maze11.alreadySeen,
        new ArrayList<Cell>(Arrays.asList(this.maze11.cells.get(4),
            this.maze11.cells.get(8))));

    this.maze11.maybeMove(3);
    this.maze11.maybeMove(1);

    t.checkExpect(this.maze11.currentIndex, 9);
    t.checkExpect(this.maze11.alreadySeen,
        new ArrayList<Cell>(Arrays.asList(this.maze11.cells.get(4),
            this.maze11.cells.get(8), this.maze11.cells.get(9))));
  }

  // Testing the search method
  void testSearch(Tester t) {
    this.initData();
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();
    cameFromCell.put(this.maze10.cells.get(3), this.maze10.cells.get(0));
    cameFromCell.put(this.maze10.cells.get(4), this.maze10.cells.get(3));
    cameFromCell.put(this.maze10.cells.get(5), this.maze10.cells.get(4));
    cameFromCell.put(this.maze10.cells.get(7), this.maze10.cells.get(4));
    cameFromCell.put(this.maze10.cells.get(6), this.maze10.cells.get(7));
    cameFromCell.put(this.maze10.cells.get(2), this.maze10.cells.get(5));
    cameFromCell.put(this.maze10.cells.get(1), this.maze10.cells.get(2));
    cameFromCell.put(this.maze10.cells.get(8), this.maze10.cells.get(5));

    ArrayList<Cell> wholePath =
        new ArrayList<Cell>(Arrays.asList(
            this.maze10.cells.get(0),
            this.maze10.cells.get(3),
            this.maze10.cells.get(4),
            this.maze10.cells.get(7),
            this.maze10.cells.get(6),
            this.maze10.cells.get(5),
            this.maze10.cells.get(2),
            this.maze10.cells.get(1),
            this.maze10.cells.get(8)));

    ArrayList<Cell> solutionPath =
        new ArrayList<Cell>(Arrays.asList(
            this.maze10.cells.get(8),
            this.maze10.cells.get(5),
            this.maze10.cells.get(4),
            this.maze10.cells.get(3),
            this.maze10.cells.get(0)));

    // 1. initial conditions
    t.checkExpect(this.maze10.path, new ArrayList<Cell>());
    t.checkExpect(this.maze10.correctPath, new ArrayList<Cell>());

    // 2. mutate
    t.checkExpect(this.maze10.search(true), cameFromCell);

    // 3. check expected results
    t.checkExpect(this.maze10.path, wholePath);
    t.checkExpect(this.maze10.correctPath, solutionPath);
  }

  // testing the reconstruct method
  void testReconstruct(Tester t) {
    this.initData();
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();
    cameFromCell.put(this.maze10.cells.get(3), this.maze10.cells.get(0));
    cameFromCell.put(this.maze10.cells.get(4), this.maze10.cells.get(3));
    cameFromCell.put(this.maze10.cells.get(5), this.maze10.cells.get(4));
    cameFromCell.put(this.maze10.cells.get(7), this.maze10.cells.get(4));
    cameFromCell.put(this.maze10.cells.get(6), this.maze10.cells.get(7));
    cameFromCell.put(this.maze10.cells.get(2), this.maze10.cells.get(5));
    cameFromCell.put(this.maze10.cells.get(1), this.maze10.cells.get(2));
    cameFromCell.put(this.maze10.cells.get(8), this.maze10.cells.get(5));

    // 1. initial conditions
    t.checkExpect(this.maze10.correctPath, new ArrayList<Cell>());

    // 2. mutate
    this.maze10.reconstruct(cameFromCell, this.maze10.cells.get(8));

    // 3. check expected results
    t.checkExpect(this.maze10.correctPath,
        new ArrayList<Cell>(Arrays.asList(
            this.maze10.cells.get(5),
            this.maze10.cells.get(4),
            this.maze10.cells.get(3),
            this.maze10.cells.get(0))));
  }

  //Testing for createMaze
  void testCreateMaze(Tester t) {
    this.initData();

    //FOR A 2X2 MAZE
    //Initialize everything except createMaze()
    this.m4.createCells();
    this.m4.connectCellsX();
    this.m4.connectCellsY();

    //Check Initial Conditions
    t.checkExpect(this.m4.cells.get(0).right,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(1), 0));
    t.checkExpect(this.m4.cells.get(0).left, null);
    t.checkExpect(this.m4.cells.get(0).top, null);
    t.checkExpect(this.m4.cells.get(0).bottom,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(2), 0));

    t.checkExpect(this.m4.cells.get(1).right, null);
    t.checkExpect(this.m4.cells.get(1).left,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(1), 0));
    t.checkExpect(this.m4.cells.get(1).top, null);
    t.checkExpect(this.m4.cells.get(1).bottom,
        new Edge(this.m4.cells.get(1), this.m4.cells.get(3), 0));

    t.checkExpect(this.m4.cells.get(2).right,
        new Edge(this.m4.cells.get(2), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(2).left, null);
    t.checkExpect(this.m4.cells.get(2).top,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(2), 0));
    t.checkExpect(this.m4.cells.get(2).bottom, null);

    t.checkExpect(this.m4.cells.get(3).right, null);
    t.checkExpect(this.m4.cells.get(3).left,
        new Edge(this.m4.cells.get(2), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(3).top,
        new Edge(this.m4.cells.get(1), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(3).bottom, null);


    //Modify
    this.m4.createMaze();

    // Check Post Conditions, most edges should be "knocked down"
    // leaving all edges except the minimum spanning tree
    t.checkExpect(this.m4.cells.get(0).right,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(1), 0));
    t.checkExpect(this.m4.cells.get(0).left, null);
    t.checkExpect(this.m4.cells.get(0).top, null);
    t.checkExpect(this.m4.cells.get(0).bottom, null);

    t.checkExpect(this.m4.cells.get(1).right, null);
    t.checkExpect(this.m4.cells.get(1).left,
        new Edge(this.m4.cells.get(0), this.m4.cells.get(1), 0));
    t.checkExpect(this.m4.cells.get(1).top, null);
    t.checkExpect(this.m4.cells.get(1).bottom,
        new Edge(this.m4.cells.get(1), this.m4.cells.get(3), 0));

    t.checkExpect(this.m4.cells.get(2).right,
        new Edge(this.m4.cells.get(2), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(2).left, null);
    t.checkExpect(this.m4.cells.get(2).top, null);
    t.checkExpect(this.m4.cells.get(2).bottom, null);

    t.checkExpect(this.m4.cells.get(3).right, null);
    t.checkExpect(this.m4.cells.get(3).left,
        new Edge(this.m4.cells.get(2), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(3).top,
        new Edge(this.m4.cells.get(1), this.m4.cells.get(3), 0));
    t.checkExpect(this.m4.cells.get(3).bottom, null);

    //FOR A 3X3 MAZE
    //Initialize everything except createMaze()
    this.m1.createCells();
    this.m1.connectCellsX();
    this.m1.connectCellsY();

    //Check Initial Conditions
    //Row 1
    t.checkExpect(this.m1.cells.get(0).right,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(0).left, null);
    t.checkExpect(this.m1.cells.get(0).top, null);
    t.checkExpect(this.m1.cells.get(0).bottom,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(3), 0));
    t.checkExpect(this.m1.cells.get(1).right,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(1).left,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(1).top, null);
    t.checkExpect(this.m1.cells.get(1).bottom,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(2).right, null);
    t.checkExpect(this.m1.cells.get(2).left,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(2).top, null);
    t.checkExpect(this.m1.cells.get(2).bottom,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    //Row 2
    t.checkExpect(this.m1.cells.get(3).right,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(3).left, null);
    t.checkExpect(this.m1.cells.get(3).top,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(3), 0));
    t.checkExpect(this.m1.cells.get(3).bottom,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(6), 0));
    t.checkExpect(this.m1.cells.get(4).right,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(4).left,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(4).top,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(4).bottom,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(5).right, null);
    t.checkExpect(this.m1.cells.get(5).left,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).top,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).bottom,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    //Row 3
    t.checkExpect(this.m1.cells.get(6).right,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(6).left, null);
    t.checkExpect(this.m1.cells.get(6).top,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(6), 0));
    t.checkExpect(this.m1.cells.get(6).bottom, null);
    t.checkExpect(this.m1.cells.get(7).right,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(7).left,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(7).top,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(7).bottom, null);
    t.checkExpect(this.m1.cells.get(8).right, null);
    t.checkExpect(this.m1.cells.get(8).left,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).top,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).bottom, null);

    //Modify
    this.m1.createMaze();

    //Check Post Conditions
    //Row 1
    t.checkExpect(this.m1.cells.get(0).right,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(0).left, null);
    t.checkExpect(this.m1.cells.get(0).top, null);
    t.checkExpect(this.m1.cells.get(0).bottom, null);
    t.checkExpect(this.m1.cells.get(1).right,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(1).left,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(1).top, null);
    t.checkExpect(this.m1.cells.get(1).bottom, null);
    t.checkExpect(this.m1.cells.get(2).right, null);
    t.checkExpect(this.m1.cells.get(2).left,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(2).top, null);
    t.checkExpect(this.m1.cells.get(2).bottom,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    //Row 2
    t.checkExpect(this.m1.cells.get(3).right,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(3).left, null);
    t.checkExpect(this.m1.cells.get(3).top, null);
    t.checkExpect(this.m1.cells.get(3).bottom, null);
    t.checkExpect(this.m1.cells.get(4).right,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(4).left,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(4).top, null);
    t.checkExpect(this.m1.cells.get(4).bottom, null);
    t.checkExpect(this.m1.cells.get(5).right, null);
    t.checkExpect(this.m1.cells.get(5).left,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).top,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).bottom,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    //Row 3
    t.checkExpect(this.m1.cells.get(6).right,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(6).left, null);
    t.checkExpect(this.m1.cells.get(6).top, null);
    t.checkExpect(this.m1.cells.get(6).bottom, null);
    t.checkExpect(this.m1.cells.get(7).right,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(7).left,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(7).top, null);
    t.checkExpect(this.m1.cells.get(7).bottom, null);
    t.checkExpect(this.m1.cells.get(8).right, null);
    t.checkExpect(this.m1.cells.get(8).left,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).top,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).bottom, null);
  }

  //Testing the connectCellsX
  void testConnectCellsX(Tester t) {
    this.initData();
    //Add the 9 cells to the maze
    this.m1.createCells();

    //Add the 6 cells to the maze
    this.m2.createCells();

    //Test initial conditions for a 3x3 maze
    //Tests each connection in m1, should all be null because nothing is initialized
    for (Cell c : this.m1.cells) {
      t.checkExpect(c.right, null);
      t.checkExpect(c.left, null);
      t.checkExpect(c.top, null);
      t.checkExpect(c.bottom, null);
    }

    //Modify
    this.m1.connectCellsX();

    //Check Post Conditions
    //Row 1
    t.checkExpect(this.m1.cells.get(0).right,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(0).left, null);
    t.checkExpect(this.m1.cells.get(0).top, null);
    t.checkExpect(this.m1.cells.get(0).bottom, null);
    t.checkExpect(this.m1.cells.get(1).right,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(1).left,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(1), 0));
    t.checkExpect(this.m1.cells.get(1).top, null);
    t.checkExpect(this.m1.cells.get(1).bottom, null);
    t.checkExpect(this.m1.cells.get(2).right, null);
    t.checkExpect(this.m1.cells.get(2).left,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(2), 0));
    t.checkExpect(this.m1.cells.get(2).top, null);
    t.checkExpect(this.m1.cells.get(2).bottom, null);
    //Row 2
    t.checkExpect(this.m1.cells.get(3).right,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(3).left, null);
    t.checkExpect(this.m1.cells.get(3).top, null);
    t.checkExpect(this.m1.cells.get(3).bottom, null);
    t.checkExpect(this.m1.cells.get(4).right,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(4).left,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(4).top, null);
    t.checkExpect(this.m1.cells.get(4).bottom, null);
    t.checkExpect(this.m1.cells.get(5).right, null);
    t.checkExpect(this.m1.cells.get(5).left,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).top, null);
    t.checkExpect(this.m1.cells.get(5).bottom, null);
    //Row 3
    t.checkExpect(this.m1.cells.get(6).right,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(6).left, null);
    t.checkExpect(this.m1.cells.get(6).top, null);
    t.checkExpect(this.m1.cells.get(6).bottom, null);
    t.checkExpect(this.m1.cells.get(7).right,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(7).left,
        new Edge(this.m1.cells.get(6), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(7).top, null);
    t.checkExpect(this.m1.cells.get(7).bottom, null);
    t.checkExpect(this.m1.cells.get(8).right, null);
    t.checkExpect(this.m1.cells.get(8).left,
        new Edge(this.m1.cells.get(7), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).top, null);
    t.checkExpect(this.m1.cells.get(8).bottom, null);

    //Test initial conditions for a 3x2 maze
    //Test each connection in m2, should all be null
    for (Cell c : this.m2.cells) {
      t.checkExpect(c.left, null);
      t.checkExpect(c.right, null);
      t.checkExpect(c.top, null);
      t.checkExpect(c.bottom, null);
    }

    //Modify
    this.m2.connectCellsX();

    //Check Post Conditions
    //Row 1
    t.checkExpect(this.m2.cells.get(0).right,
        new Edge(this.m2.cells.get(0), this.m2.cells.get(1), 0));
    t.checkExpect(this.m2.cells.get(0).left, null);
    t.checkExpect(this.m2.cells.get(0).top, null);
    t.checkExpect(this.m2.cells.get(0).bottom, null);
    t.checkExpect(this.m2.cells.get(1).right, null);
    t.checkExpect(this.m2.cells.get(1).left,
        new Edge(this.m2.cells.get(0), this.m2.cells.get(1), 0));
    t.checkExpect(this.m2.cells.get(1).top, null);
    t.checkExpect(this.m2.cells.get(1).bottom, null);
    //Row 2
    t.checkExpect(this.m2.cells.get(2).right,
        new Edge(this.m2.cells.get(2), this.m2.cells.get(3), 0));
    t.checkExpect(this.m2.cells.get(2).left, null);
    t.checkExpect(this.m2.cells.get(2).top, null);
    t.checkExpect(this.m2.cells.get(2).bottom, null);
    t.checkExpect(this.m2.cells.get(3).right, null);
    t.checkExpect(this.m2.cells.get(3).left,
        new Edge(this.m2.cells.get(2), this.m2.cells.get(3), 0));
    t.checkExpect(this.m2.cells.get(3).top, null);
    t.checkExpect(this.m2.cells.get(3).bottom, null);
    //Row 3
    t.checkExpect(this.m2.cells.get(4).right,
        new Edge(this.m2.cells.get(4), this.m2.cells.get(5), 0));
    t.checkExpect(this.m2.cells.get(4).left, null);
    t.checkExpect(this.m2.cells.get(4).top, null);
    t.checkExpect(this.m2.cells.get(4).bottom, null);
    t.checkExpect(this.m2.cells.get(5).right, null);
    t.checkExpect(this.m2.cells.get(5).left,
        new Edge(this.m2.cells.get(4), this.m2.cells.get(5), 0));
    t.checkExpect(this.m2.cells.get(5).top, null);
    t.checkExpect(this.m2.cells.get(5).bottom, null);
  }

  //Testing the connectCellsY
  void testConnectCellsY(Tester t) {
    this.initData();
    //Add the 9 cells to the maze
    this.m1.createCells();

    //Add the 6 cells to the maze
    this.m2.createCells();

    //Test initial conditions for a 3x3 maze
    //Tests each connection in m1, should all be null because nothing is initialized
    for (Cell c : this.m1.cells) {
      t.checkExpect(c.right, null);
      t.checkExpect(c.left, null);
      t.checkExpect(c.top, null);
      t.checkExpect(c.bottom, null);
    }

    //Modify
    this.m1.connectCellsY();

    //Check Post Conditions
    //Row 1
    t.checkExpect(this.m1.cells.get(0).right, null);
    t.checkExpect(this.m1.cells.get(0).left, null);
    t.checkExpect(this.m1.cells.get(0).top, null);
    t.checkExpect(this.m1.cells.get(0).bottom,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(3), 0));
    t.checkExpect(this.m1.cells.get(1).right, null);
    t.checkExpect(this.m1.cells.get(1).left, null);
    t.checkExpect(this.m1.cells.get(1).top, null);
    t.checkExpect(this.m1.cells.get(1).bottom,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(2).right, null);
    t.checkExpect(this.m1.cells.get(2).left, null);
    t.checkExpect(this.m1.cells.get(2).top, null);
    t.checkExpect(this.m1.cells.get(2).bottom,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    //Row 2
    t.checkExpect(this.m1.cells.get(3).right, null);
    t.checkExpect(this.m1.cells.get(3).left, null);
    t.checkExpect(this.m1.cells.get(3).top,
        new Edge(this.m1.cells.get(0), this.m1.cells.get(3), 0));
    t.checkExpect(this.m1.cells.get(3).bottom,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(6), 0));
    t.checkExpect(this.m1.cells.get(4).right, null);
    t.checkExpect(this.m1.cells.get(4).left, null);
    t.checkExpect(this.m1.cells.get(4).top,
        new Edge(this.m1.cells.get(1), this.m1.cells.get(4), 0));
    t.checkExpect(this.m1.cells.get(4).bottom,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(5).right, null);
    t.checkExpect(this.m1.cells.get(5).left, null);
    t.checkExpect(this.m1.cells.get(5).top,
        new Edge(this.m1.cells.get(2), this.m1.cells.get(5), 0));
    t.checkExpect(this.m1.cells.get(5).bottom,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    //Row 3
    t.checkExpect(this.m1.cells.get(6).right, null);
    t.checkExpect(this.m1.cells.get(6).left, null);
    t.checkExpect(this.m1.cells.get(6).top,
        new Edge(this.m1.cells.get(3), this.m1.cells.get(6), 0));
    t.checkExpect(this.m1.cells.get(6).bottom, null);
    t.checkExpect(this.m1.cells.get(7).right, null);
    t.checkExpect(this.m1.cells.get(7).left, null);
    t.checkExpect(this.m1.cells.get(7).top,
        new Edge(this.m1.cells.get(4), this.m1.cells.get(7), 0));
    t.checkExpect(this.m1.cells.get(7).bottom, null);
    t.checkExpect(this.m1.cells.get(8).right, null);
    t.checkExpect(this.m1.cells.get(8).left, null);
    t.checkExpect(this.m1.cells.get(8).top,
        new Edge(this.m1.cells.get(5), this.m1.cells.get(8), 0));
    t.checkExpect(this.m1.cells.get(8).bottom, null);

    //Test initial conditions for a 3x2 maze
    //Test each connection in m2, should all be null
    for (Cell c : this.m2.cells) {
      t.checkExpect(c.left, null);
      t.checkExpect(c.right, null);
      t.checkExpect(c.top, null);
      t.checkExpect(c.bottom, null);
    }

    //Modify
    this.m2.connectCellsY();

    //Check Post Conditions
    //Row 1
    t.checkExpect(this.m2.cells.get(0).right, null);
    t.checkExpect(this.m2.cells.get(0).left, null);
    t.checkExpect(this.m2.cells.get(0).top, null);
    t.checkExpect(this.m2.cells.get(0).bottom,
        new Edge(this.m2.cells.get(0), this.m2.cells.get(2), 0));
    t.checkExpect(this.m2.cells.get(1).right, null);
    t.checkExpect(this.m2.cells.get(1).left, null);
    t.checkExpect(this.m2.cells.get(1).top, null);
    t.checkExpect(this.m2.cells.get(1).bottom,
        new Edge(this.m2.cells.get(1), this.m2.cells.get(3), 0));
    //Row 2
    t.checkExpect(this.m2.cells.get(2).right, null);
    t.checkExpect(this.m2.cells.get(2).left, null);
    t.checkExpect(this.m2.cells.get(2).top,
        new Edge(this.m2.cells.get(0), this.m2.cells.get(2), 0));
    t.checkExpect(this.m2.cells.get(2).bottom,
        new Edge(this.m2.cells.get(2), this.m2.cells.get(4), 0));
    t.checkExpect(this.m2.cells.get(3).right, null);
    t.checkExpect(this.m2.cells.get(3).left, null);
    t.checkExpect(this.m2.cells.get(3).top,
        new Edge(this.m2.cells.get(1), this.m2.cells.get(3), 0));
    t.checkExpect(this.m2.cells.get(3).bottom,
        new Edge(this.m2.cells.get(3), this.m2.cells.get(5), 0));
    //Row 3
    t.checkExpect(this.m2.cells.get(4).right, null);
    t.checkExpect(this.m2.cells.get(4).left, null);
    t.checkExpect(this.m2.cells.get(4).top,
        new Edge(this.m2.cells.get(2), this.m2.cells.get(4), 0));
    t.checkExpect(this.m2.cells.get(4).bottom, null);
    t.checkExpect(this.m2.cells.get(5).right, null);
    t.checkExpect(this.m2.cells.get(5).left, null);
    t.checkExpect(this.m2.cells.get(5).top,
        new Edge(this.m2.cells.get(3), this.m2.cells.get(5), 0));
    t.checkExpect(this.m2.cells.get(5).bottom, null);
  }

  // testing the createCells method
  void testCreateCells(Tester t) {
    this.initData();
    Cell c1 = new Cell(new Posn(0, 0));
    Cell c2 = new Cell(new Posn(1, 0));
    Cell c3 = new Cell(new Posn(2, 0));
    Cell c4 = new Cell(new Posn(0, 1));
    Cell c5 = new Cell(new Posn(1, 1));
    Cell c6 = new Cell(new Posn(2, 1));
    Cell c7 = new Cell(new Posn(0, 2));
    Cell c8 = new Cell(new Posn(1, 2));
    Cell c9 = new Cell(new Posn(2, 2));
    ArrayList<Cell> cellsExpected =
        new ArrayList<Cell>(Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9));

    // 1. initial conditions
    t.checkExpect(this.maze13.width, 3);
    t.checkExpect(this.maze13.height, 3);
    t.checkExpect(this.maze13.rand, new Random(0));
    t.checkExpect(this.maze13.cells, new ArrayList<Cell>());
    t.checkExpect(this.maze13.edges, new ArrayList<Edge>());
    t.checkExpect(this.maze13.cellSize, 200);

    // 2. mutate
    this.maze13.createCells();

    // 3. check expected results
    t.checkExpect(this.maze13.width, 3);
    t.checkExpect(this.maze13.height, 3);
    t.checkExpect(this.maze13.rand, new Random(0));
    t.checkExpect(this.maze13.cells, cellsExpected);
    t.checkExpect(this.maze13.edges, new ArrayList<Edge>());
    t.checkExpect(this.maze13.cellSize, 200);
  }

  // testing the makeScene method
  void testMakeScene(Tester t) {
    this.initData();

    WorldScene s10Expected = new WorldScene(900, 600);
    s10Expected = this.maze10.drawBorderAndBackground(s10Expected);
    s10Expected = this.maze10.drawStartAndEnd(s10Expected);

    for (Cell c : this.maze10.cells) {
      s10Expected = c.drawCell(s10Expected, this.maze10.cellSize);
    }

    WorldScene s11Expected = new WorldScene(900, 600);
    s11Expected = this.maze11.drawBorderAndBackground(s11Expected);
    s11Expected = this.maze11.drawStartAndEnd(s11Expected);

    for (Cell c : this.maze11.cells) {
      s11Expected = c.drawCell(s11Expected, this.maze11.cellSize);
    }

    WorldScene s12Expected = new WorldScene(900, 600);
    s12Expected = this.maze12.drawBorderAndBackground(s12Expected);
    s12Expected = this.maze12.drawStartAndEnd(s12Expected);

    for (Cell c : this.maze12.cells) {
      s12Expected = c.drawCell(s12Expected, this.maze12.cellSize);
    }

    t.checkExpect(this.maze10.makeScene(), s10Expected);
    t.checkExpect(this.maze11.makeScene(), s11Expected);
    t.checkExpect(this.maze12.makeScene(), s12Expected);
  }

  // testing the drawBorderAndBackground method
  void testDrawBorderAndBackground(Tester t) {
    this.initData();

    WorldScene s10 = new WorldScene(900, 600);
    WorldScene s10Expected = new WorldScene(900, 600);
    WorldImage b10 = new RectangleImage(600, 600, "outline", Color.BLACK);
    WorldImage bg10 = new RectangleImage(600, 600, "solid", new Color(192, 192, 192));
    s10Expected.placeImageXY(bg10, 300, 300);
    s10Expected.placeImageXY(b10, 300, 300);

    WorldScene s11 = new WorldScene(900, 600);
    WorldScene s11Expected = new WorldScene(900, 600);
    WorldImage b11 = new RectangleImage(600, 600, "outline", Color.BLACK);
    WorldImage bg11 = new RectangleImage(600, 600, "solid", new Color(192, 192, 192));
    s11Expected.placeImageXY(bg11, 300, 300);
    s11Expected.placeImageXY(b11, 300, 300);

    WorldScene s12 = new WorldScene(900, 600);
    WorldScene s12Expected = new WorldScene(900, 600);
    WorldImage b12 = new RectangleImage(900, 360, "outline", Color.BLACK);
    WorldImage bg12 = new RectangleImage(900, 360, "solid", new Color(192, 192, 192));
    s12Expected.placeImageXY(bg12, 450, 180);
    s12Expected.placeImageXY(b12, 450, 180);

    t.checkExpect(this.maze10.drawBorderAndBackground(s10), s10Expected);
    t.checkExpect(this.maze11.drawBorderAndBackground(s11), s11Expected);
    t.checkExpect(this.maze12.drawBorderAndBackground(s12), s12Expected);
  }

  // testing the drawStartAndEnd method
  void testDrawStartAndEnd(Tester t) {
    this.initData();

    WorldScene s10 = new WorldScene(900, 600);
    WorldScene s10Expected = new WorldScene(900, 600);
    WorldImage t10 = new RectangleImage(200 - 1, 200 - 1, "solid", new Color(102, 204, 0));
    WorldImage b10 = new RectangleImage(200 - 1, 200 - 1, "solid", new Color(153, 0, 153));
    s10Expected.placeImageXY(t10, 101, 101);
    s10Expected.placeImageXY(b10, 501, 501);

    WorldScene s11 = new WorldScene(900, 600);
    WorldScene s11Expected = new WorldScene(900, 600);
    WorldImage t11 = new RectangleImage(150 - 1, 150 - 1, "solid", new Color(102, 204, 0));
    WorldImage b11 = new RectangleImage(150 - 1, 150 - 1, "solid", new Color(153, 0, 153));
    s11Expected.placeImageXY(t11, 76, 76);
    s11Expected.placeImageXY(b11, 526, 526);

    WorldScene s12 = new WorldScene(900, 600);
    WorldScene s12Expected = new WorldScene(900, 600);
    WorldImage t12 = new RectangleImage(180 - 1, 180 - 1, "solid", new Color(102, 204, 0));
    WorldImage b12 = new RectangleImage(180 - 1, 180 - 1, "solid", new Color(153, 0, 153));
    s12Expected.placeImageXY(t12, 91, 91);
    s12Expected.placeImageXY(b12, 811, 271);

    t.checkExpect(this.maze10.drawStartAndEnd(s10), s10Expected);
    t.checkExpect(this.maze11.drawStartAndEnd(s11), s11Expected);
    t.checkExpect(this.maze12.drawStartAndEnd(s12), s12Expected);
  }

  // TESTS FOR THE EDGE CLASS
  // testing the removeEdge method
  void testRemoveEdge(Tester t) {
    this.initData();
    this.cell1.right = this.edge1;
    this.cell2.left = this.edge1;
    this.cell3.bottom = this.edge2;
    this.cell4.top = this.edge2;
    this.cell5.right = this.edge3;
    this.cell6.left = this.edge3;

    // 1. initial conditions
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, this.edge1);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, this.edge1);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 2. mutate
    this.edge1.removeEdge();

    // 3. check expected results
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, this.edge2);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, this.edge2);
    t.checkExpect(this.cell4.bottom, null);

    // 2. mutate
    this.edge2.removeEdge();

    // 3. check expected results
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, this.edge3);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, this.edge3);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);

    // 2. mutate
    this.edge3.removeEdge();

    // 3. check expected results
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, this.edge3);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, this.edge3);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);
  }

  // TESTS FOR THE CELL CLASS
  // testing the updateLeft method
  void testUpdateLeft(Tester t) {
    this.initData();

    // 1. initial conditions
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 2. mutate
    this.cell1.updateLeft(this.edge1);

    // 3. check expected results
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, this.edge1);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 2. mutate
    this.cell3.updateLeft(this.edge2);

    // 3. check expected results
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, this.edge2);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);

    // 2. mutate
    this.cell5.updateLeft(this.edge3);

    // 3. check expected results
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, this.edge3);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);
  }

  // testing the updateRight method
  void testUpdateRight(Tester t) {
    this.initData();

    // 1. initial conditions
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 2. mutate
    this.cell1.updateRight(this.edge1);

    // 3. check expected results
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, this.edge1);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 2. mutate
    this.cell3.updateRight(this.edge2);

    // 3. check expected results
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, this.edge2);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);

    // 2. mutate
    this.cell5.updateRight(this.edge3);

    // 3. check expected results
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, this.edge3);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);
  }

  // testing the updateTop method
  void testUpdateTop(Tester t) {
    this.initData();

    // 1. initial conditions
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 2. mutate
    this.cell1.updateTop(this.edge1);

    // 3. check expected results
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, this.edge1);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 2. mutate
    this.cell3.updateTop(this.edge2);

    // 3. check expected results
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, this.edge2);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);

    // 2. mutate
    this.cell5.updateTop(this.edge3);

    // 3. check expected results
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, this.edge3);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);
  }

  // testing the updateBottom method
  void testUpdateBottom(Tester t) {
    this.initData();

    // 1. initial conditions
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, null);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 2. mutate
    this.cell1.updateBottom(this.edge1);

    // 3. check expected results
    t.checkExpect(this.cell1.coord, new Posn(1, 2));
    t.checkExpect(this.cell1.left, null);
    t.checkExpect(this.cell1.right, null);
    t.checkExpect(this.cell1.top, null);
    t.checkExpect(this.cell1.bottom, this.edge1);

    t.checkExpect(this.cell2.coord, new Posn(10, 2));
    t.checkExpect(this.cell2.left, null);
    t.checkExpect(this.cell2.right, null);
    t.checkExpect(this.cell2.top, null);
    t.checkExpect(this.cell2.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, null);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 2. mutate
    this.cell3.updateBottom(this.edge2);

    // 3. check expected results
    t.checkExpect(this.cell3.coord, new Posn(5, 6));
    t.checkExpect(this.cell3.left, null);
    t.checkExpect(this.cell3.right, null);
    t.checkExpect(this.cell3.top, null);
    t.checkExpect(this.cell3.bottom, this.edge2);

    t.checkExpect(this.cell4.coord, new Posn(5, 7));
    t.checkExpect(this.cell4.left, null);
    t.checkExpect(this.cell4.right, null);
    t.checkExpect(this.cell4.top, null);
    t.checkExpect(this.cell4.bottom, null);

    // 1. initial conditions
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, null);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);

    // 2. mutate
    this.cell5.updateBottom(this.edge3);

    // 3. check expected results
    t.checkExpect(this.cell5.coord, new Posn(2, 4));
    t.checkExpect(this.cell5.left, null);
    t.checkExpect(this.cell5.right, null);
    t.checkExpect(this.cell5.top, null);
    t.checkExpect(this.cell5.bottom, this.edge3);

    t.checkExpect(this.cell6.coord, new Posn(7, 7));
    t.checkExpect(this.cell6.left, null);
    t.checkExpect(this.cell6.right, null);
    t.checkExpect(this.cell6.top, null);
    t.checkExpect(this.cell6.bottom, null);
  }

  // testing the horizontalBetween method
  void testHorizontalBetween(Tester t) {
    this.initData();
    t.checkExpect(this.cell1.horizontalBetween(this.cell2), true);
    t.checkExpect(this.cell2.horizontalBetween(this.cell1), true);
    t.checkExpect(this.cell3.horizontalBetween(this.cell4), false);
    t.checkExpect(this.cell1.horizontalBetween(this.cell3), false);
    t.checkExpect(this.cell5.horizontalBetween(this.cell6), false);
  }

  // testing the verticalBetween method
  void testVerticalBetween(Tester t) {
    this.initData();
    t.checkExpect(this.cell3.verticalBetween(this.cell4), true);
    t.checkExpect(this.cell4.verticalBetween(this.cell3), true);
    t.checkExpect(this.cell1.verticalBetween(this.cell2), false);
    t.checkExpect(this.cell3.verticalBetween(this.cell5), false);
    t.checkExpect(this.cell5.verticalBetween(this.cell6), false);
  }

  // testing the drawCell method
  void testDrawCell(Tester t) {
    this.initData();
    this.cell1.right = this.edge1;
    this.cell1.top = this.edge1;
    this.cell1.bottom = this.edge1;

    this.cell2.left = this.edge2;
    this.cell2.top = this.edge2;
    this.cell2.bottom = this.edge2;

    this.cell3.left = this.edge3;
    this.cell3.right = this.edge3;
    this.cell3.bottom = this.edge3;

    this.cell4.left = this.edge3;
    this.cell4.right = this.edge3;
    this.cell4.top = this.edge3;

    WorldScene s1 = new WorldScene(900, 600);
    WorldScene s1Expected = new WorldScene(900, 600);
    s1Expected.placeImageXY(
        new LineImage(new Posn(0, 10), Color.BLACK),
        (1 * 10),
        (2 * 10) + 10 / 2);

    WorldScene s2 = new WorldScene(900, 600);
    WorldScene s2Expected = new WorldScene(900, 600);
    s2Expected.placeImageXY(
        new LineImage(new Posn(0, 4), Color.BLACK),
        ((10 * 4) + 4),
        (2 * 4) + 4 / 2);

    WorldScene s3 = new WorldScene(900, 600);
    WorldScene s3Expected = new WorldScene(900, 600);
    s3Expected.placeImageXY(
        new LineImage(new Posn(20, 0), Color.BLACK),
        (5 * 20) + 20 / 2,
        (6 * 20));

    WorldScene s4 = new WorldScene(900, 600);
    WorldScene s4Expected = new WorldScene(900, 600);
    s4Expected.placeImageXY(
        new LineImage(new Posn(50, 0), Color.BLACK),
        (5 * 50) + 50 / 2,
        (7 * 50) + 50);

    t.checkExpect(this.cell1.drawCell(s1, 10), s1Expected);
    t.checkExpect(this.cell2.drawCell(s2, 4), s2Expected);
    t.checkExpect(this.cell3.drawCell(s3, 20), s3Expected);
    t.checkExpect(this.cell4.drawCell(s4, 50), s4Expected);
  }

  // testing the addNeighbors method
  void testAddNeighbors(Tester t) {
    this.initData();
    Deque<Cell> worklist = new ArrayDeque<Cell>();
    ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();

    Deque<Cell> worklistE = new ArrayDeque<Cell>();
    worklistE.addFirst(this.maze10.cells.get(3));

    HashMap<Cell, Cell> cameFromCellE = new HashMap<Cell, Cell>();
    cameFromCellE.put(this.maze10.cells.get(3), this.maze10.cells.get(0));

    // 1. initial conditions
    t.checkExpect(worklist, new ArrayDeque<Cell>());
    t.checkExpect(alreadySeen, new ArrayList<Cell>());
    t.checkExpect(cameFromCell, new HashMap<Cell, Cell>());

    // 2. mutate
    this.maze10.cells.get(0).addNeighbors(worklist, alreadySeen, cameFromCell, true);

    // 3. check expected results
    t.checkExpect(worklist, worklistE);
    t.checkExpect(alreadySeen, new ArrayList<Cell>());
    t.checkExpect(cameFromCell, cameFromCellE);
  }

  // TESTS FOR THE UNION/FIND CLASS

  // testing the initRepresentatives method
  void testInitRepresentatives(Tester t) {
    this.initData();

    ArrayList<Cell> arr1 = new ArrayList<Cell>(Arrays.asList(this.cell1, this.cell2));
    ArrayList<Cell> arr2 =
        new ArrayList<Cell>(Arrays.asList(this.cell1, this.cell2, this.cell3));
    ArrayList<Cell> arr3 = new ArrayList<Cell>(Arrays.asList(this.cell4,
        this.cell2, this.cell55, this.cell3, this.cell1, this.cell66));

    //Test Initial Condition
    t.checkExpect(this.uf.representatives.get(this.cell1), null);
    t.checkExpect(this.uf.representatives.get(this.cell2), null);
    t.checkExpect(this.uf.representatives.size(), 0);

    //Modify
    this.uf.initRepresentatives(arr1);

    //Test post conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell1);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.size(), 2);

    //Reset representatives for another test
    this.uf.representatives = new HashMap<Cell, Cell>();

    //Test initial conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), null);
    t.checkExpect(this.uf.representatives.get(this.cell2), null);
    t.checkExpect(this.uf.representatives.get(this.cell3), null);
    t.checkExpect(this.uf.representatives.size(), 0);

    //Modify
    this.uf.initRepresentatives(arr2);

    //Check post conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell1);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell3), this.cell3);
    t.checkExpect(this.uf.representatives.size(), 3);

    //Reset representatives for another test
    this.uf.representatives = new HashMap<Cell, Cell>();

    //Check initial conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), null);
    t.checkExpect(this.uf.representatives.get(this.cell2), null);
    t.checkExpect(this.uf.representatives.get(this.cell3), null);
    t.checkExpect(this.uf.representatives.get(this.cell55), null);
    t.checkExpect(this.uf.representatives.get(this.cell66), null);
    t.checkExpect(this.uf.representatives.size(), 0);

    //Modify
    this.uf.initRepresentatives(arr3);

    //Check the post conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell1);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell4), this.cell4);
    t.checkExpect(this.uf.representatives.get(this.cell55), this.cell55);
    t.checkExpect(this.uf.representatives.size(), 6);

  }

  // testing the find method
  void testFind(Tester t) {
    this.initData();

    ArrayList<Cell> arr3 = new ArrayList<Cell>(Arrays.asList(this.cell4, this.cell2,
        this.cell55, this.cell3, this.cell1, this.cell66));

    //Initialize the representatives
    this.uf.initRepresentatives(arr3);

    //Each should return the same as the given because each cell is its own representative
    t.checkExpect(this.uf.find(this.cell4), this.cell4);
    t.checkExpect(this.uf.find(this.cell2), this.cell2);
    t.checkExpect(this.uf.find(this.cell55), this.cell55);
    t.checkExpect(this.uf.find(this.cell3), this.cell3);

    //Modify so that some cells have other representatives
    this.uf.representatives.put(this.cell4, this.cell2);
    this.uf.representatives.put(this.cell2, this.cell1);
    this.uf.representatives.put(this.cell1, this.cell66);

    //Check After
    t.checkExpect(this.uf.find(this.cell4), this.cell66);
    t.checkExpect(this.uf.find(this.cell2), this.cell66);
    t.checkExpect(this.uf.find(this.cell1), this.cell66);

    //Modify again
    this.uf.representatives.put(this.cell55, this.cell1);
    this.uf.representatives.put(this.cell4, this.cell3);
    this.uf.representatives.put(this.cell2, this.cell2);

    //Check after
    t.checkExpect(this.uf.find(this.cell1), this.cell66);
    t.checkExpect(this.uf.find(this.cell2), this.cell2);
    t.checkExpect(this.uf.find(this.cell3), this.cell3);
    t.checkExpect(this.uf.find(this.cell4), this.cell3);
    t.checkExpect(this.uf.find(this.cell55), this.cell66);
    t.checkExpect(this.uf.find(this.cell66), this.cell66);
  }

  // testing the union method
  void testUnion(Tester t) {
    this.initData();

    ArrayList<Cell> arr3 = new ArrayList<Cell>(Arrays.asList(this.cell4,
        this.cell2, this.cell55, this.cell3, this.cell1, this.cell66));
    //Initialize representatives
    this.uf.initRepresentatives(arr3);

    //Check initial conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell1);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell3), this.cell3);
    t.checkExpect(this.uf.representatives.get(this.cell4), this.cell4);
    t.checkExpect(this.uf.representatives.get(this.cell55), this.cell55);
    t.checkExpect(this.uf.representatives.get(this.cell66), this.cell66);

    //Modify
    this.uf.union(this.cell2, this.cell3);
    this.uf.union(this.cell4, this.cell55);
    this.uf.union(this.cell1, this.cell66);
    this.uf.union(this.cell3, this.cell4);
    this.uf.union(this.cell55, this.cell2);

    //Check After
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell3);
    t.checkExpect(this.uf.representatives.get(this.cell4), this.cell55);
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell66);
    t.checkExpect(this.uf.representatives.get(this.cell3), this.cell55);
    t.checkExpect(this.uf.representatives.get(this.cell55), this.cell55);

    //Reset for more tests
    this.uf.representatives = new HashMap<Cell, Cell>();
    this.uf.initRepresentatives(arr3);

    //Check initial conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell1);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell3), this.cell3);
    t.checkExpect(this.uf.representatives.get(this.cell4), this.cell4);
    t.checkExpect(this.uf.representatives.get(this.cell55), this.cell55);
    t.checkExpect(this.uf.representatives.get(this.cell66), this.cell66);

    //Modify
    this.uf.union(this.cell1, this.cell2);
    this.uf.union(this.cell4, this.cell2);
    this.uf.union(this.cell3, this.cell1);
    this.uf.union(this.cell55, this.cell66);

    //Check Post Conditions
    t.checkExpect(this.uf.representatives.get(this.cell1), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell2), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell3), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell4), this.cell2);
    t.checkExpect(this.uf.representatives.get(this.cell55), this.cell66);
    t.checkExpect(this.uf.representatives.get(this.cell66), this.cell66);
  }
}
