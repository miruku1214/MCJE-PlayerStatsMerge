package com.MirukuStudio.MCJEPlayerStatsMerge;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

public class Main {
    private static final String DATA_VERSION_KEY = "DataVersion";
    private static final String STATS_KEY = "stats";
    private static final String VERSION = "1.0-1.20.4";

    public static void main(String[] args) {
        Options options = new Options();

        Option optInputs = new Option("i", "inputs", true, "Target statistics JSON files");
        optInputs.setArgName("FILES");
        optInputs.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(optInputs);

        Option optOutput = new Option("o", "output", true, "Output statistics JSON file");
        optOutput.setArgName("FILE");
        options.addOption(optOutput);

        Option optHelp = new Option("h", "help", false, "Show help");
        options.addOption(optHelp);

        Option optVersion = new Option("v", "version", false, "Show version");
        options.addOption(optVersion);

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(optHelp)) {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("MCJE-PlayerStatsMerge.jar", options);
            } else if (cmd.hasOption(optVersion)) {
                System.out.println(VERSION);
            } else {
                if (!cmd.hasOption(optInputs)) {
                    throw new ParseException("No input files!");
                } else {
                    if (cmd.getOptionValues(optInputs).length < 2) {
                        throw new ParseException("Not enough input files!");
                    }
                }
                if (!cmd.hasOption(optOutput)) {
                    throw new ParseException("No output file!");
                }

                String[] inputFilePaths = cmd.getOptionValues(optInputs);
                String outputFilePath = cmd.getOptionValue(optOutput);

                try {
                    JsonObject jsonDataMerged = new JsonObject();
                    JsonObject statsMerged = new JsonObject();
                    jsonDataMerged.add(STATS_KEY, statsMerged);

                    for (String filePath: inputFilePaths) {
                        BufferedReader reader = new BufferedReader(new FileReader(filePath));

                        JsonElement jsonElement = JsonParser.parseReader(reader);
                        JsonObject jsonData = jsonElement.getAsJsonObject();

                        JsonObject stats = jsonData.getAsJsonObject(STATS_KEY);
                        for (String categoryKey: stats.keySet()) {
                            if (!statsMerged.has(categoryKey)) {
                                statsMerged.add(categoryKey, new JsonObject());
                            }
                            JsonObject categoryMerged = statsMerged.getAsJsonObject(categoryKey);
                            JsonObject category = stats.getAsJsonObject(categoryKey);

                            for (String statKey: category.keySet()) {
                                JsonPrimitive statValuePrimitive = category.getAsJsonPrimitive(statKey);
                                int statValue = statValuePrimitive.getAsInt();
                                if (!categoryMerged.has(statKey)) {
                                    categoryMerged.add(statKey, new JsonPrimitive(statValue));
                                } else {
                                    JsonPrimitive statValuePrimitiveMerged = categoryMerged.getAsJsonPrimitive(statKey);
                                    int statValueMerged = statValuePrimitiveMerged.getAsInt();
                                    categoryMerged.add(statKey, new JsonPrimitive(statValueMerged + statValue));
                                }
                            }
                        }

                        JsonPrimitive dataVersionPrimitive = jsonData.getAsJsonPrimitive(DATA_VERSION_KEY);
                        int dataVersion = dataVersionPrimitive.getAsInt();
                        if (!jsonDataMerged.has(DATA_VERSION_KEY)) {
                            jsonDataMerged.add(DATA_VERSION_KEY, new JsonPrimitive(dataVersion));
                        } else {
                            JsonPrimitive dataVersionPrimitiveMerged = jsonDataMerged.getAsJsonPrimitive(DATA_VERSION_KEY);
                            int dataVersionMerged = dataVersionPrimitiveMerged.getAsInt();
                            if (dataVersionMerged < dataVersion) {
                                jsonDataMerged.add(DATA_VERSION_KEY, new JsonPrimitive(dataVersion));
                            }
                        }
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
                    Gson gson = new Gson();
                    String mergedStatsJson = gson.toJson(jsonDataMerged);
                    writer.write(mergedStatsJson);
                    writer.flush();
                    writer.close();
                } catch (IOException ex) {
                    System.out.println("IOException: " + ex.getMessage());
                }
            }
        } catch (ParseException ex) {
            System.out.println("Argument Error: " + ex.getMessage());
        }
    }
}
