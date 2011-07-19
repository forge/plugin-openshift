/*
 *  Copyright 2010 Red Hat, Inc.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
package com.redhat.openshift.utils;

import java.lang.reflect.Method;
import java.util.List;

import org.jboss.forge.shell.plugins.PipeOut;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class Formatter {
	private String repeat(String pattern, int times){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<times;i++)
			sb.append(pattern);
		return sb.toString();
	}
	
	private void printRowDelim(int[] columnSizes, int indent, PipeOut out){
		out.print(repeat("    ", indent));
		out.print("+");
		for(int i=0;i<columnSizes.length;i++){
			out.print(repeat("-", columnSizes[i] + 2));
			out.print("+");
		}
		out.println();
	}
	
	public void printTable( String[] columnHeader, String[] fieldNames, int[] columnSizes, List<?> data, int indent, PipeOut out) throws Exception{
		printRowDelim(columnSizes, indent, out);
		out.print(repeat("    ", indent));
		out.print("| ");
		for(int i=0;i<columnHeader.length;i++){
			out.print(String.format("%" + columnSizes[i] + "s ", columnHeader[i]));
			out.print("| ");
		}
		out.println();
		printRowDelim(columnSizes, indent, out);
		
		for (Object object : data) {
			out.print(repeat("    ", indent));
			out.print("| ");
			for(int i=0;i<fieldNames.length;i++){
				Method mth = object.getClass().getMethod("get" + fieldNames[i], new Class[]{});
				String str = mth.invoke(object).toString();
				out.print(String.format("%" + columnSizes[i] + "s ", str));
				out.print("| ");
			}
			out.println();
		}
		printRowDelim(columnSizes, indent, out);
	}

	public void printTable(String[] columnHeader, String[] fieldNames, int[] columnSizes, Object object, int indent, PipeOut out){
		printRowDelim(columnSizes, indent, out);
		out.print(repeat("    ", indent));
		out.print("| ");
		for(int i=0;i<columnHeader.length;i++){
			out.print(String.format("%" + columnSizes[i] + "s ", columnHeader[i]));
			out.print("| ");
		}
		out.println();
		printRowDelim(columnSizes, indent, out);
		
		out.print(repeat("    ", indent));
		out.print("| ");
		for(int i=0;i<fieldNames.length;i++){
			Method mth;
			String str = "";
			try {
				mth = object.getClass().getMethod("get" + fieldNames[i], new Class[]{});
				str = mth.invoke(object).toString();				
			} catch (Exception e){
				//supress
			}
			out.print(String.format("%" + columnSizes[i] + "s ", str));
			out.print("| ");
		}
		out.println();
		printRowDelim(columnSizes, indent, out);
	}
}
