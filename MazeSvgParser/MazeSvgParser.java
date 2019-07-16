import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

public class MazeSvgParser 
{
	private static final String PARSED_EXTENSION = ".csv";
	
	private static final String PARSED_DIR = "Parsed";
	
	public static void main(String[] args) throws Exception 
	{
		Args parsedArgs = parseArgs(args);
		
		ConvertMazesFromSvgToCsv(parsedArgs.svgMazeDir, parsedArgs.width, parsedArgs.height);
	}
	
	private static void ConvertMazesFromSvgToCsv(String svgMazeDir, int width, int height) throws Exception
	{
		String parsedDirAbsoltutePath = getPathFromRelativePath(PARSED_DIR);
		File parsedDir = null;
		try
		{
			parsedDir = new File(parsedDirAbsoltutePath);
			parsedDir.mkdirs();
		}
		catch (SecurityException e)
		{
			System.err.println("Error caught while attempting to create " + PARSED_DIR + ": " + e.getMessage());
			System.exit(3);
		}
		
		String loadDirAbsolute = getPathFromRelativePath(svgMazeDir);
		
		File loadDir = new File(loadDirAbsolute);
		
		if (!loadDir.exists())
		{
			System.err.println("Load directory " + loadDirAbsolute + " does not exist. \nPlease provide a path to a relative directory under the program's executing directory");
			System.exit(4);
		}
		
		// https://www.java2novice.com/java-file-io-operations/file-list-by-file-filter/
		File[] mazeFileArray = loadDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) 
				{
					if(name.toLowerCase().endsWith(".svg"))
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			});
		
		if (mazeFileArray.length > 0)
		{
			System.out.println("Found " + mazeFileArray.length + " .svg files in ./" + svgMazeDir);
			System.out.println("Parsing files and saving to ./" + PARSED_DIR);
		}
		else
		{
			System.out.println("No .svg files found in ./" + svgMazeDir + ". Quitting.");
			System.exit(0);
		}
		
		int countSuccessfullyParsed = 0;
		for (int i = 0; i < mazeFileArray.length; i++)
		{
			String mazeFileName = mazeFileArray[i].getName();
			String mazeAbsoluteFilePath = mazeFileArray[i].getAbsolutePath();
			
			Maze.Info mazeInfo = null;
			try 
			{
				mazeInfo = Maze.LoadMazeInfoFromSVG(mazeAbsoluteFilePath, width, height);
			}
			catch (Exception e) 
			{
				System.err.println("Error caught while loading file " + mazeFileName + ": " + e.getMessage() + "\n");
				// in this case, continue on to the next file, since there's nothing left to do with this one
				continue;
			}
			try
			{
				SaveParsedMazeData(fileNameWithoutExtension(mazeFileName), mazeInfo);
				countSuccessfullyParsed++;
			}
			catch (Exception e)
			{
				System.err.println("Error caught while saving maze info for " + mazeFileName + ": " + e.getMessage() + "\n");
			}
		}
		
		System.out.println("Done. " + countSuccessfullyParsed + " / " + mazeFileArray.length + " files successfully parsed.");
	}
	
	private static String fileNameWithoutExtension(String withExtension)
	{
		String[] split = withExtension.split("\\.");
		String rtn = split[0];
		for (int i = 1; i < (split.length - 1); i++)
		{
			rtn += split[i];
		}
		return rtn;
	}
	
	private static void SaveParsedMazeData(String origFileNameNoExtension, Maze.Info mazeInfo) throws Exception
	{
		if (mazeInfo == null)
		{
			throw new Exception("Error caught while attempting to save parsed data for " + origFileNameNoExtension + ": Maze info is empty");
		}
		
		String saveDirAbsolute = getPathFromRelativePath(PARSED_DIR);
		
		String saveFilePath = saveDirAbsolute + File.separator + origFileNameNoExtension + PARSED_EXTENSION;
		
		StringBuilder sb = new StringBuilder();
		
		// First two lines are the entrance and exit (I picked the bottom as entrance and top as exit, but i don't think it matters)
		sb.append(mazeInfo.Entrance.X);
		sb.append(",");
		sb.append(mazeInfo.Entrance.Y);
		sb.append("\n");
		
		sb.append(mazeInfo.Exit.X);
		sb.append(",");
		sb.append(mazeInfo.Exit.Y);
		sb.append("\n");
		
		// Now save a row for each coordinate that starts with the coordinate, and is followed by all unreachable neighbors
		for (Maze.Coord coord: mazeInfo.UnreachableNeighbors.keySet())
		{
			AppendCoordAndUnreachableNeighbors(coord, mazeInfo, sb);
		}
		
		SaveToFile(sb.toString(), saveFilePath);
	}
	
	private static void AppendCoordAndUnreachableNeighbors(Maze.Coord coordinate, Maze.Info mazeInfo, StringBuilder sb)
	{
		sb.append(coordinate.X);
		sb.append(",");
		sb.append(coordinate.Y);
		for (Maze.Coord neighbor: mazeInfo.UnreachableNeighbors.get(coordinate))
		{
			sb.append(",");
			sb.append(neighbor.X);
			sb.append(",");
			sb.append(neighbor.Y);
		}
		sb.append("\n");
	}
	
	private static void SaveToFile(String data, String fileName) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		
		writer.write(data);
		
		writer.close();
	}
	
	protected static String getPathFromRelativePath(String relativePath)
	{
		String executingDir = new File("").getAbsolutePath();
		return executingDir + File.separator + relativePath;
	}
	
	private static Args parseArgs(String[] args) throws Exception
	{
		if (args.length != 3)
		{
			System.err.println(Args.NUM_ARGS_EXCEPTION_MSG);
			System.exit(1);
		}
		
        String svgMazeDir = args[0];
        try
        {
        	int width = Integer.parseInt(args[1]);
            int height = Integer.parseInt(args[2]);
            Args rtnArgs = new Args(svgMazeDir, width, height);
    		
    		return rtnArgs;
        }
        catch (Exception e)
        {
        	System.err.println("Exception caught while attempting to parse arguments 2 or 3. Should be integers. Error message: " + e.getMessage());
        	System.exit(2);
        	return null;  // we'll never get here, but my eclipse editor doesn't know that somehow
        }
	}
	
	/*
	 * Wrapper for returning command line arguments
	 */
	static class Args 
	{
		public static final String NUM_ARGS_EXCEPTION_MSG = 
				"You must provide 3 arguments: \n" +
				"1. Relative path to directory where maze svg files are saved\n" + 
				"2. Width of mazes in the directory\n" +
				"3. Height of mazes in the directory\n" + 
				"Example from unix command line:\n" + 
				"java MazeSvgParser \"Mazes\" 50 47";
		
        String svgMazeDir;
        int width;
        int height;
        
        // constructor 
        public Args(String imageFileDir, int width, int height) 
        {
            this.svgMazeDir = imageFileDir; 
            this.width = width;
            this.height = height;
        }
    }
}
