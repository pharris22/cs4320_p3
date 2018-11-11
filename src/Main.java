/*
 * CS 4320
 * Spring 2018
 * Peter Harris
 * Project 3
 */

import java.util.Random;

class Item {
	/* Maximum values for weight and value are flexible and can be changed. */
	public static double weight_max = 50;
	public static double value_max = 100;
	public double weight;
	public double value;
	public Item(Random rand) {
		/* Floor function here truncates the doubles to 3 decimal places, for readability. */
		weight = (Math.floor(rand.nextDouble() * weight_max * 1000) / 1000);
		value = (Math.floor(rand.nextDouble() * value_max * 1000) / 1000);
	}
}

public class Main {
	/* Each of these global variables are also changeable, to yield different results. */
	public static int pop_size = 40;
	public static int num_items = 100;
	public static int k = 50; /* Sets penalty value */
	public static double pc = 0.6; /* Probability of crossover */
	public static double pm = 0.004; /* Probability of permutation */
	public static int totalGens = 100000;
	
	/* Uses the same seed each time for repeatability */
	public static long seedNum = 1525210359242L;
	public static Random rand = new Random(seedNum);
	
	/* Uncomment this section (and comment out the last section) for random seeding instead. */
	//public static long time = java.lang.System.currentTimeMillis();
	//public static Random rand = new Random(time);
	
	public static double weight_lim; /* weight_lim is dynamically generated to change according to the values of the items */
	public static Item[] item_arr = new Item[num_items];
	public static int[][] knapsack_arr = new int[pop_size][num_items];
	public static int[] total_best = new int[num_items];
	
	public static void main(String[] args) {
		/* Displays the seed used, if doing random seeding */
		//System.out.println("Seed: " + time + "L");
		pop_arrs();
		display_items();
		display_knapsack();
		do_everything();
	}
	
	/* Generates values/weights for the items, and creates the random population strings. */
	public static void pop_arrs() {
		for (int i = 0; i < num_items; i++) {
			total_best[i] = 0;
		}
		for (int i = 0; i < num_items; i++) {
			item_arr[i] = new Item(rand);
		}
		for (int i = 0; i < pop_size; i++) {
			for (int j = 0; j < num_items; j++) {
				knapsack_arr[i][j] = rand.nextInt(2);
			}
		}
		sort_arr();
		double total_value = 0;
		for (int i = 0; i < num_items; i++) {
			total_value += item_arr[i].value;
		}
		total_value = Math.floor(total_value * 1000) / 1000;
		System.out.println("Total value: " + total_value);
		double total_weight = 0;
		for (int i = 0; i < num_items; i++) {
			total_weight += item_arr[i].weight;
		}
		total_weight = Math.floor(total_weight * 1000) / 1000;
		System.out.println("Total weight: " + total_weight);
		set_weight_lim(total_weight);
	}
	
	/* Sets the weight limit. Set to be half of the total item weight, rounded down to the next 10. */
	public static void set_weight_lim(double total_weight) {
		weight_lim = (Math.floor((total_weight / 2) / 10) * 10);
	}
	
	/* Does most of the rest of the function calls. */
	public static void do_everything() {
		for (int i = 0; i < totalGens; i++) {
			selection();
			crossover();
			mutation();
			find_total_best();
			if (i % (totalGens / 5) == 0) {
				System.out.println("Generation: " + i + ".....................");
				display_knapsack();
			}
		}
		System.out.println("End of program items strings:");
		display_knapsack();
		System.out.println("Weight limit: " + weight_lim);
	}
	
	/* Implements binary tournament selection, using the penalty method for fitness calculations. */
	public static void selection() {
		int[][] new_knapsack_arr = new int[pop_size][num_items];
		for (int i = 0; i < pop_size; i++) {
			int place1 = rand.nextInt(pop_size), place2 = rand.nextInt(pop_size);
			if (get_fitness(get_value(place1), get_weight(place1)) > get_fitness(get_value(place2), get_weight(place2))) {
				for (int j = 0; j < num_items; j++) {
					new_knapsack_arr[i][j] = knapsack_arr[place1][j];
				}
			}
			else if (get_fitness(get_value(place1), get_weight(place1)) < get_fitness(get_value(place2), get_weight(place2))) {
				for (int j = 0; j < num_items; j++) {
					new_knapsack_arr[i][j] = knapsack_arr[place2][j];
				}
			}
			else {
				if (rand.nextBoolean()) {
					for (int j = 0; j < num_items; j++) {
						new_knapsack_arr[i][j] = knapsack_arr[place1][j];
					}
				}
				else {
					for (int j = 0; j < num_items; j++) {
						new_knapsack_arr[i][j] = knapsack_arr[place2][j];
					}
				}
			}
		}
		for (int i = 0; i < pop_size; i++) {
			for (int j = 0; j < num_items; j++) {
				knapsack_arr[i][j] = new_knapsack_arr[i][j];
			}
		}
	}

	/* Single-point random-place crossover. */
	public static void crossover() {
		for (int i = 0; i < pop_size; i++) {
			int num = rand.nextInt(10000);
			if (num < (pc * 10000)) {
				int cross2 = rand.nextInt(pop_size);
				int loc = rand.nextInt(num_items);
				for (int j = loc; j < num_items; j++) {
					int temp = knapsack_arr[i][j];
					knapsack_arr[i][j] = knapsack_arr[cross2][j];
					knapsack_arr[cross2][j] = temp;
				}
			}
		}
	}
	
	/* Bit-string mutation. */
	public static void mutation() {
		for (int i = 0; i < pop_size; i++) {
			for (int j = 0; j < num_items; j++) {
				int num = rand.nextInt(10000);
				if (num < (pm * 10000)) {
					if (knapsack_arr[i][j] == 0) {
						knapsack_arr[i][j] = 1;
					}
					else {
						knapsack_arr[i][j] = 0;
					}
				}
			}
		}
	}
	
	/* Piece-wise function for returning the fitness, using penalty. */
	public static double get_fitness(double value, double weight) {
		if (weight <= weight_lim) {
			return (Math.floor(value * 1000) / 1000);
		}
		else {
			return (Math.floor((value - k * (weight - weight_lim)) * 1000) / 1000);
		}
	}
	
	/* Overloaded function for reusability in other situations. */
	public static double get_fitness(int[] arr) {
		double weight = 0;
		double value = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == 1) {
				weight += item_arr[i].weight;
				value += item_arr[i].value;
			}
		}
		if (weight <= weight_lim) {
			return (Math.floor(value * 1000) / 1000);
		}
		else {
			return (Math.floor((value - k * (weight - weight_lim)) * 1000) / 1000);
		}
	}
	
	/* Returns the weight of the given population item. */
	public static double get_weight(int pop_num) {
		double weight = 0;
		for (int i = 0; i < num_items; i++) {
			if (knapsack_arr[pop_num][i] == 1) {
				weight += item_arr[i].weight;
			}
		}	
		return (Math.floor(weight * 1000) / 1000);
	}
	
	public static double get_weight(int [] arr) {
		double weight = 0;
		for (int i = 0; i < num_items; i++) {
			if (arr[i] == 1) {
				weight += item_arr[i].weight;
			}
		}	
		return (Math.floor(weight * 1000) / 1000);
	}
	
	/* Returns the value of the given population item. */
	public static double get_value(int pop_num) {
		double value = 0;
		for (int i = 0; i < num_items; i++) {
			if (knapsack_arr[pop_num][i] == 1) {
				value += item_arr[i].value;
			}
		}
		return (Math.floor(value * 1000) / 1000);
	}
	
	public static double get_value(int [] arr) {
		double value = 0;
		for (int i = 0; i < num_items; i++) {
			if (arr[i] == 1) {
				value += item_arr[i].value;
			}
		}
		return (Math.floor(value * 1000) / 1000);
	}
	
	/* Sorts the array according to the fitness of each population item. */
	public static void sort_arr() {
		Boolean swapped = true;
		while (swapped) {
			swapped = false;
			for (int i = 0; i < (pop_size - 1); i++) {
				if (get_fitness(get_value(i), get_weight(i)) > get_fitness(get_value(i+1), get_weight(i+1))) {
					swapped = true;
					for (int j = 0; j < num_items; j++) {
						int temp = knapsack_arr[i][j];
						knapsack_arr[i][j] = knapsack_arr[i+1][j];
						knapsack_arr[i+1][j] = temp;
					}
				}
			}
		}
	}
	
	/* Displays the weight and value of each item. */
	public static void display_items() {
		System.out.println("Item values/weights:...............................");
		for (int i = 0; i < num_items; i++) {
			System.out.println(i + ": " + item_arr[i].value + " " + item_arr[i].weight);
		}
	}
	
	/* Displays the best string out of the current generation. */
	public static void current_best() {
		int[] current_best = new int[num_items];
		for (int i = 0; i < num_items; i++) {
			current_best[i] = 0;
		}
		for (int i = 0; i < pop_size; i++) {
			if (get_fitness(current_best) < get_fitness(knapsack_arr[i])) {
				for (int j = 0; j < num_items; j++) {
					current_best[j] = knapsack_arr[i][j];
				}
			}
		}
		System.out.print("Current best: ");
		for (int i = 0; i < num_items; i++) {
			System.out.print(current_best[i]);
		}
		System.out.println(", value: " + get_value(current_best) + ", weight: " + get_weight(current_best) + ", fitness: " + get_fitness(current_best));
	}
	
	/* Updates the overall best string found, if necessary. */
	public static void find_total_best() {
		for (int i = 0; i < pop_size; i++) {
			if (get_fitness(get_value(i), get_weight(i)) > get_fitness(total_best)) {
				for (int j = 0; j < num_items; j++) {
					total_best[j] = knapsack_arr[i][j];
				}
			}
		}
	}
	
	/* Displays the string, weight, value, and fitness of each population item. */
	public static void display_knapsack() {
		sort_arr();		
		find_total_best();
		System.out.println("Item strings........................................");
		double avg_weight = 0, avg_value = 0, avg_fitness = 0;
		for (int i = 0; i < pop_size; i++) {
			System.out.print(i + ": ");
			for (int j = 0; j < num_items; j++) {
				System.out.print(knapsack_arr[i][j]);
			}
			avg_weight += get_weight(i);
			avg_value += get_value(i);
			avg_fitness += get_fitness(get_value(i), get_weight(i));
			System.out.println(", value: " + get_value(i) + ", weight: " + get_weight(i) + ", fitness: " + get_fitness(get_value(i), get_weight(i)));
		}
		avg_weight /= pop_size;
		avg_weight = Math.floor(avg_weight * 1000) / 1000;
		avg_value /= pop_size;
		avg_value = Math.floor(avg_value * 1000) / 1000;
		avg_fitness /= pop_size;
		avg_fitness = Math.floor(avg_fitness * 1000) / 1000;
		System.out.println("Average weight: " + avg_weight);
		System.out.println("Average value: " + avg_value);
		System.out.println("Average fitness: " + avg_fitness);
		current_best();
		System.out.print("Overall best: ");
		for (int i = 0; i < num_items; i++) {
			System.out.print(total_best[i]);
		}
		System.out.println(", value: " + get_value(total_best) + ", weight: " + get_weight(total_best) + ", fitness: " + get_fitness(total_best));
	}
}


