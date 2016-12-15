package com.meituan.android.walle.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.meituan.android.walle.ChannelWriter;
import com.meituan.android.walle.internal.SignatureNotFoundException;
import com.meituan.android.walle.utils.CommaSeparatedKeyValueConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Parameters(commandDescription = "channel apk batch production")
public class WriteChannelsCommand implements IWalleCommand{

    @Parameter(required = true, description = "inputFile [outputDirectory]", arity = 2, converter = FileConverter.class)
    private List<File> files;

    @Parameter(names = {"-e", "--extraInfo"}, converter = CommaSeparatedKeyValueConverter.class, description = "Comma-separated list of key=value info, eg: -e time=1,type=android")
    private Map<String, String> extraInfo;

    @Parameter(names = {"-c", "--channelList"}, description = "Comma-separated list of channel, eg: -c meituan,xiaomi")
    private List<String> channelList;

    @Parameter(names = {"-f", "--channelFile"}, description = "channel file")
    private File channelFile;

    @Override
    public void parse() {
        File inputFile = files.get(0);
        File outputDir = null;
        if (files.size() == 2) {
            outputDir = files.get(1);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
        } else {
            outputDir = inputFile.getParentFile();
        }

        if (channelList != null) {
            for (String channel : channelList) {
                generateChannelApk(inputFile, outputDir, channel);
            }
        }

        if (channelFile != null) {
            try {
                List<String> lines = IOUtils.readLines(new FileInputStream(channelFile), "UTF-8");
                for (String line : lines) {
                    String channel = line.split("#")[0].trim();
                    generateChannelApk(inputFile, outputDir, channel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateChannelApk(File inputFile, File outputDir, String channel) {
        String name = FilenameUtils.getBaseName(inputFile.getName());
        String extension = FilenameUtils.getExtension(inputFile.getName());
        String newName = name + "_" + channel + "." + extension;
        File channelApk = new File(outputDir, newName);
        try {
            FileUtils.copyFile(inputFile, channelApk);
            ChannelWriter.put(channelApk, channel, extraInfo);
        } catch (IOException | SignatureNotFoundException e) {
            e.printStackTrace();
        }
    }
}