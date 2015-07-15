package learn.genenet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Encodings
{

	private int							numBins;
	private Map<Integer, double[]>		discreteSpecies;
	private List<List<List<Integer>>>	levelAssignments;

	public Encodings()
	{
		numBins = 3;
		discreteSpecies = new HashMap<Integer, double[]>();
		levelAssignments = new ArrayList<List<List<Integer>>>();
	}

	public Encodings(int bin)
	{
		numBins = bin;
		discreteSpecies = new HashMap<Integer, double[]>();
		levelAssignments = new ArrayList<List<List<Integer>>>();
	}

	public void addDiscreteSpecies(int col, double[] values)
	{
		discreteSpecies.put(col, values);
	}

	public void addLevelAssignment(int experiment, int row, int col, double data)
	{
		while (levelAssignments.size() < experiment + 1)
		{
			levelAssignments.add(new ArrayList<List<Integer>>());
		}
		while (levelAssignments.get(experiment).size() < row + 1)
		{
			levelAssignments.get(experiment).add(new ArrayList<Integer>());
		}
		while (levelAssignments.get(experiment).get(row).size() < col + 1)
		{
			levelAssignments.get(experiment).get(row).add(0);
		}
		levelAssignments.get(experiment).get(row).set(col, getLevelAssignment(col, data));
	}

	public int getLevelAssignment(int col, double data)
	{
		double[] discrete = discreteSpecies.get(col);

		for (int i = 1; i < discrete.length; i++)
		{
			if (data <= discrete[i])
			{
				return i - 1;
			}
		}

		return numBins - 1;
	}

	public List<List<List<Integer>>> getLevelAssignments()
	{
		return levelAssignments;
	}

	public int size()
	{
		return levelAssignments.size();
	}

	public void print()
	{
		for (int i = 0; i < levelAssignments.size(); i++)
		{
			System.out.println("Experiment :" + i);
			for (int j = 0; j < levelAssignments.get(i).size(); j++)
			{
				for (int k = 1; k < levelAssignments.get(i).get(j).size(); k++)
				{
					System.out.print(levelAssignments.get(i).get(j).get(k) + " ");
				}
				System.out.println("");
			}

		}
	}

}
