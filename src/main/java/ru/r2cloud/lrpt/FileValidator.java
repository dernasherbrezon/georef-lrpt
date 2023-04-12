package ru.r2cloud.lrpt;

import java.io.File;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class FileValidator implements IParameterValidator {

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!new File(value).exists()) {
			throw new ParameterException("Cannot find the file: " + value + " for " + name);
		}
	}

}
