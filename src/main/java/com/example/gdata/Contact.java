package com.example.gdata;

import com.googlecode.jcsv.annotations.MapToColumn;

public class Contact {
	@MapToColumn(column = 0)
	public String fullName;

	@MapToColumn(column = 1)
	public String email;

	@MapToColumn(column = 2)
	public String phone;
}
