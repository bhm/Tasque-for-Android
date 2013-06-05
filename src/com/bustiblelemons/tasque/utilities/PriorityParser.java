package com.bustiblelemons.tasque.utilities;


import com.bustiblelemons.tasque.utilities.Values.Database.Task;

import it.bova.rtmapi.Priority;

public class PriorityParser {

	public static Priority parse(int priority) {
		switch (priority) {
		case Task.Priority.High:
			return Priority.HIGH;
		case Task.Priority.Medium:
			return Priority.MEDIUM;
		case Task.Priority.Low:
			return Priority.LOW;
		default:
			return Priority.NONE;
		}
	}

}
