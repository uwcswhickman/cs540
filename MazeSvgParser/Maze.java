import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Maze 
{
	public static Info LoadMazeInfoFromSVG(String svgFileName, int width, int height) throws Exception
	{
		// build list of x,y pairs that represent each grid block in the maze
		Coord[][] coordinates = CoordinateStarter(width, height);
		
		// Load all of the lines from the svg file and create Barrier objects, which can then calculate which neighbors are separated by it
		List<Barrier> barriers = GetMazeBarriers(svgFileName, width, height);
		
		// gather list of coordinates that are on the bottom row of the maze - this assumes that you want the entrance to be on the bottom
		List<Coord> entranceCandidates = EntranceCandidates(coordinates);
		// gather list of coordinates that are on the top row of the maze - this assumes that you want the exit to be on the top
		List<Coord> exitCandidates = ExitCandidates(coordinates);
		
		List<Pair> unreachableNeighborsList = new LinkedList<Pair>();
		// for each barrier, calculate who's separated by it (if anyone; edges won't separate any maze blocks)
		for (Barrier barrier : barriers)
		{
			unreachableNeighborsList.addAll(barrier.GetEstrangedNeighbors(coordinates));
			// if the barrier is a top or bottom edge, eliminate all blocks that are adjacent to it from our entrance/exit candidates
			barrier.PruneEntranceCandidates(coordinates, entranceCandidates);
			barrier.PruneExitCandidates(coordinates, exitCandidates);
		}
		
		// for each coordinate, keep track of the set of neighbors that are unreachable
		Map<Coord, Set<Coord>> unreachableNeighborMap = new LinkedHashMap<Coord, Set<Coord>>();
		
		for (int x = 0; x < coordinates.length; x++)
		{
			for (int y = 0; y < coordinates[x].length; y++)
			{
				// Collection of unreachable neighbors is a Set so that we don't have to deal with whether the neighbor has already been added to list - it'll take care of that for us
				unreachableNeighborMap.put(coordinates[x][y], new HashSet<Coord>());
			}
		}
		
		// for each pair, add one to the other's list of unreachable neighbors
		for (Pair pair: unreachableNeighborsList)
		{
			unreachableNeighborMap.get(pair.A).add(pair.B);
			unreachableNeighborMap.get(pair.B).add(pair.A);
		}
		
		if (entranceCandidates.size() > 1)
		{
			throw new Exception("More than one entrance candidate found");
		}
		if (exitCandidates.size() > 1)
		{
			throw new Exception("More than one exit candidate found");
		}
		
		// only one left, so we have a winner
		Coord entrance = entranceCandidates.get(0);
		// same here
		Coord exit = exitCandidates.get(0);
		
		Info mazeInfo = new Info(unreachableNeighborMap, entrance, exit);
		
		return mazeInfo;
	}
	
	public static List<Maze.Barrier> GetMazeBarriers(String fileName, int width, int height) throws Exception 
	{
		List<Element> lineElements = GetLineElementsFromSVG(fileName);
		
		List<Maze.Barrier> rtnList = new LinkedList<Maze.Barrier>();
		
		for (Element line: lineElements)
		{
			rtnList.add(new Maze.Barrier(line, width, height));
		}
		
		return rtnList;
	}
	
	public static List<Element> GetLineElementsFromSVG(String fileName) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		
		Document doc = builder.parse(fileName);
		Element rootElement = doc.getDocumentElement();
		Element gElement = GetGElement(rootElement);
		return GetLineElements(gElement);
	}
	
	private static Element GetGElement(Element rootElement) throws Exception
	{
		NodeList children = rootElement.getChildNodes();
		Node current = null;
		int count = children.getLength();
		for (int i = 0; i < count; i++) 
		{
			current = children.item(i);
			if (current.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element element = (Element) current;
				if (element.getTagName().equalsIgnoreCase("g")) 
				{
					return element;
				}
			}
		}
		
		throw new Exception("No child elements found with tag name == \"g\"");
	}
	
	private static List<Element> GetLineElements(Element gElement)
	{
		List<Element> rtnElements = new LinkedList<Element>();
		
		NodeList children = gElement.getChildNodes();
		Node current = null;
		int count = children.getLength();
		for (int i = 0; i < count; i++) 
		{
			current = children.item(i);
			if (current.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element element = (Element) current;
				if (element.getTagName().equalsIgnoreCase("line")) 
				{
					rtnElements.add(element);
				}
			}
		}
		
		return rtnElements;
	}
	
	private static List<Coord> EntranceCandidates(Coord[][] coordinates)
	{
		List<Coord> rtnList = new LinkedList<Coord>();
		
		for (int x = 0; x < coordinates.length; x++)
		{
			rtnList.add(coordinates[x][0]);
		}
		
		return rtnList;
	}
	
	private static List<Coord> ExitCandidates(Coord[][] coordinates)
	{
		List<Coord> rtnList = new LinkedList<Coord>();
		
		int topRow = coordinates[0].length - 1;
		for (int x = 0; x < coordinates.length; x++)
		{
			rtnList.add(coordinates[x][topRow]);
		}
		
		return rtnList;
	}
	
	public static Coord[][] CoordinateStarter(int width, int height)
	{
		Coord[][] coordinates = new Coord[width][];
		
		for (int x = 0; x < width; x++)
		{
			coordinates[x] = new Coord[height];
			for (int y = 0; y < height; y++)
			{
				coordinates[x][y] = new Coord(x, y);
			}
		}
		
		return coordinates;
	}
	
	static class Info
	{
		public Map<Coord, Set<Coord>> UnreachableNeighbors;
		public Coord Entrance;
		public Coord Exit;
		public Coord[][] Coords;
		
		public Info(Map<Coord, Set<Coord>> unreachableNeighbors, Coord entrance, Coord exit)
		{
			this.UnreachableNeighbors = unreachableNeighbors;
			this.Entrance = entrance;
			this.Exit = exit;
		}
	}
	
	static class Pair
	{
		public Coord A; 
		public Coord B;
	}
	
	static class Coord
	{
		public int X;
		public int Y;
		
		public Coord(int x, int y)
		{
			this.X = x;
			this.Y = y;
		}
		
		@Override
		public String toString()
		{
			return "(" + this.X + ", " + this.Y + ")";
		}
	}
	
	static class Barrier 
	{
		public enum Orientation
		{
			Horizontal,
			Vertical
		}
		
		private static final String X1_ATTRIBUTE_NAME = "x1";
		private static final String Y1_ATTRIBUTE_NAME = "y1";
		private static final String X2_ATTRIBUTE_NAME = "x2";
		private static final String Y2_ATTRIBUTE_NAME = "y2";
		private static final int MAZE_BLOCK_SIZE = 16;  // see below for explanation
		private static final int MAZE_OFFSET_SIZE = 2;  // see below
		
		// raw data from SVG file's line element that this barrier represents
		// Example element:
		// <line x1="2" y1="2" x2="66" y2="2" />
		int X1;  // "x1" attribute from line element (2 in the example above)
		int Y1;  // "y1" attribute
		int X2;  // etc.
		int Y2;
		
		// each attribute is shifted by 2, and each grid block is 16x16, so we subtract 2 and divide by 16 to get it scaled down
		int scaledDownStartX;  // scaled down version of X1. Ex: If x1="2", then this is 0. 
		int scaledDownStartY;
		int scaledDownEndX;    // If x2="66", then this is 4
		int scaledDownEndY;
		Orientation orientation;  // Horizontal or Vertical. If Y1 == Y2, then it's horizontal. If X1 == X2, then it's vertical
		
		public Barrier(Element domElement, int mazeWidth, int mazeHeight) throws Exception
		{
			if (!isLineElement(domElement))
			{
				throw new Exception("Dom element is not a line");
			}
			
			parseXYAttributes(domElement);
			
			calculateProperties(mazeWidth, mazeHeight);
		}
		
		private void calculateProperties(int mazeWidth, int mazeHeight)
		{
			if (this.X1 == this.X2)
			{
				this.orientation = Orientation.Vertical;
			}
			else
			{
				this.orientation = Orientation.Horizontal;
			}
			
			this.scaledDownStartX = (this.X1 - MAZE_OFFSET_SIZE) / MAZE_BLOCK_SIZE;
			this.scaledDownEndX = (this.X2 - MAZE_OFFSET_SIZE) / MAZE_BLOCK_SIZE;
			this.scaledDownStartY = mazeHeight - (this.Y2 - MAZE_OFFSET_SIZE) / MAZE_BLOCK_SIZE;
			this.scaledDownEndY = mazeHeight - ((this.Y1 - MAZE_OFFSET_SIZE) / MAZE_BLOCK_SIZE);
			
		}
		
		private void parseXYAttributes(Element domElement)
		{
			this.X1 = Integer.parseInt(domElement.getAttribute(X1_ATTRIBUTE_NAME));
			this.Y1 = Integer.parseInt(domElement.getAttribute(Y1_ATTRIBUTE_NAME));
			this.X2 = Integer.parseInt(domElement.getAttribute(X2_ATTRIBUTE_NAME));
			this.Y2 = Integer.parseInt(domElement.getAttribute(Y2_ATTRIBUTE_NAME));
		}
		
		private boolean isLineElement(Element toCheck)
		{
			return toCheck.getTagName().equalsIgnoreCase("line");
		}
		
		/**
		 * Returns a list of pairs of grid blocks that are separated by this barrier
		 * For example, if this barrier is 2 blocks high and it's between the first and second columns, then the returned list would be two pairs:
		 * Pair 1: (0,0) & (1,0)  // first and second coordinates on the first row
		 * Pair 2: (0,1) & (1,1)  // first and second coordinates on the second row
		 * @param coordinates
		 * @return
		 */
		public List<Pair> GetEstrangedNeighbors(Coord[][] coordinates)
		{
			List<Pair> unreachableNeighborPairs = new LinkedList<Pair>();
			
			// in this case, we're looking for neighbors to the left and right who are separated by this line
			if (this.orientation == Orientation.Vertical)
			{
				int xIdx = this.scaledDownStartX;
				int topYMazeIdx = this.scaledDownEndY - 1;	// y index of the first block affected by this barrier
				int bottomYMazeIdx = this.scaledDownStartY; // y index of the last block affected by this barrier

				// if maze index is 0 or it's the max, then it's a left or right edge, so it isn't separating anyone
				if ((xIdx != 0) && (xIdx != coordinates.length))
				{
					int leftMazeIdx = xIdx - 1;  // x index of blocks immediately to the left of this barrier
					int rightMazeIdx = xIdx;     // x index of blocks immediately to the right of this barrier
					
					for (int y = bottomYMazeIdx; y <= topYMazeIdx; y++)
					{
						Pair estrangedPair = new Pair();
						estrangedPair.A = coordinates[leftMazeIdx][y];
						estrangedPair.B = coordinates[rightMazeIdx][y];
						unreachableNeighborPairs.add(estrangedPair);
					}
				}
			}
			// otherwise, looking for neighbors above and below
			else
			{
				int yIdx = this.scaledDownStartY;
				int leftXMazeIdx = this.scaledDownStartX;		// x index of the first block affected by this barrier
				int rightXMazeIdx = this.scaledDownEndX - 1;  	// x index of the last block affected by this barrier
							
				// if maze index is 0 or it's the max, then it's a top or bottom edge, so it isn't separating anyone
				if ((yIdx != 0) && (yIdx != coordinates[0].length))
				{
					int bottomMazeIdx = yIdx - 1;   // y index of blocks immediately above this barrier
					int topMazeIdx = yIdx;			// y index of blocks immediately below this barrier
					
					for (int x = leftXMazeIdx; x <= rightXMazeIdx; x++)
					{
						Pair estrangedPair = new Pair();
						estrangedPair.A = coordinates[x][bottomMazeIdx];
						estrangedPair.B = coordinates[x][topMazeIdx];
						unreachableNeighborPairs.add(estrangedPair);
					}
				}
			}
			
			return unreachableNeighborPairs;
		}
		
		/**
		 * Given this barrier's properties, see if we can rule out any entrance candidates from the bottom row
		 * Only does something if this line is on the bottom edge
		 * @param coordinates
		 * @param entranceCandidates
		 */
		public void PruneEntranceCandidates(Coord[][] coordinates, List<Coord> entranceCandidates)
		{
			if (this.IsBottomEdge())
			{
				// if it's the bottom edge, then everyone that's next to it is blocked from the outside, so they can all be eliminated as candidates for the entrance
				int leftXMazeIdx = this.scaledDownStartX;	// the x index of the first coordinate that would be ruled out on the bottom row by this barrier
				int rightXMazeIdx = this.scaledDownEndX - 1;  // the x index of the last coordinate that would be ruled out on the bottom row by this barrier
				
				for (int x = leftXMazeIdx; x <= rightXMazeIdx; x++)
				{
					entranceCandidates.remove(coordinates[x][0]);
				}
			}
		}
		/**
		 * Given this barrier's properties, see if we can rule out any exit candidates from the top row
		 * Only does something if this line is on the top edge
		 * @param coordinates
		 * @param exitCandidates
		 */
		public void PruneExitCandidates(Coord[][] coordinates, List<Coord> exitCandidates)
		{
			if (this.IsTopEdge(coordinates[0].length))
			{
				// if it's the top edge, then everyone that's next to it is blocked from the outside, so they can all be eliminated as candidates for the exit
				int y = coordinates[0].length - 1;  // the y index of the top row of coordinates
				int leftXMazeIdx = this.scaledDownStartX;  // the x index of the first coordinate that would be ruled out on the top row
				int rightXMazeIdx = this.scaledDownEndX - 1;  // the x index of the last coordinate that would be ruled out on the top row
				
				for (int x = leftXMazeIdx; x <= rightXMazeIdx; x++)
				{
					exitCandidates.remove(coordinates[x][y]);
				}
			}
		}
		
		public boolean IsBottomEdge()
		{
			if (this.orientation == Orientation.Horizontal)
			{
				if (this.scaledDownStartY == 0)
				{
					return true;
				}
			}
			return false;
		}
		
		public boolean IsTopEdge(int height)
		{
			if (this.orientation == Orientation.Horizontal)
			{
				if (this.scaledDownStartY == height)
				{
					return true;
				}
			}
			return false;
		}
	}
}
