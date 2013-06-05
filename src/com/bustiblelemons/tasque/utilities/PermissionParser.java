package com.bustiblelemons.tasque.utilities;

import it.bova.rtmapi.Permission;

public class PermissionParser {

	public static Permission parse(String permission) {
		if (permission.equalsIgnoreCase(Permission.DELETE.toString())) {
			return Permission.DELETE;
		} else if (permission.equalsIgnoreCase(Permission.WRITE.toString())) {
			return Permission.WRITE;
		} else if (permission.equalsIgnoreCase(Permission.READ.toString())) {
			return Permission.READ;
		} else {
			return Permission.NONE;
		}
	}
}
