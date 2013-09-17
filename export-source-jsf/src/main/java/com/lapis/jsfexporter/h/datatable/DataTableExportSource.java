/*
 * #%L
 * Lapis JSF Exporter - PrimeFaces export sources
 * %%
 * Copyright (C) 2013 Lapis Software Associates
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.lapis.jsfexporter.h.datatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;

import com.lapis.jsfexporter.api.FacetType;
import com.lapis.jsfexporter.api.IExportCell;
import com.lapis.jsfexporter.api.IExportType;
import com.lapis.jsfexporter.h.util.JsfUtil;
import com.lapis.jsfexporter.impl.ExportCellImpl;
import com.lapis.jsfexporter.impl.ExportRowImpl;
import com.lapis.jsfexporter.spi.IExportSource;
import com.lapis.jsfexporter.util.ExportUtil;

public class DataTableExportSource implements IExportSource<HtmlDataTable, DataTableExportOptions> {

	private static final Map<FacetType, List<String>> FACET_NAMES;
	
	static {
		Map<FacetType, List<String>> facetNamesTemp = new HashMap<FacetType, List<String>>();
		facetNamesTemp.put(FacetType.HEADER, Collections.unmodifiableList(Arrays.asList("header")));
		facetNamesTemp.put(FacetType.FOOTER, Collections.unmodifiableList(Arrays.asList("footer")));
		FACET_NAMES = facetNamesTemp;
	}
	
	@Override
	public Class<HtmlDataTable> getSourceType() {
		return HtmlDataTable.class;
	}

	@Override
	public DataTableExportOptions getDefaultConfigOptions() {
		return new DataTableExportOptions();
	}
	
	@Override
	public int getColumnCount(HtmlDataTable source, DataTableExportOptions configOptions) {
		int columnCount = 0;
		for (UIComponent kid : source.getChildren()) {
			if (kid instanceof UIColumn && kid.isRendered()) {
				columnCount++;
			}
		}
		return columnCount;
	}

	@Override
	public void exportData(HtmlDataTable source, DataTableExportOptions configOptions, IExportType<?, ?, ?> exporter, FacesContext context) throws Exception {
		List<UIColumn> columns = new ArrayList<UIColumn>();
		for (UIComponent kid : source.getChildren()) {
			if (kid instanceof UIColumn && kid.isRendered()) {
				columns.add((UIColumn) kid);
			}
		}
		
		List<List<String>> columnNames = exportFacet(FacetType.HEADER, source, columns, exporter, context);
		
		if (configOptions.getRange() == DataTableExportRange.ALL) {
			exportRowCells(source, columns, columnNames, 0, source.getRowCount(), exporter, context);
		} else { // PAGE_ONLY
			exportRowCells(source, columns, columnNames, source.getFirst(), source.getFirst() + source.getRows(), exporter, context);
		}
		
		exportFacet(FacetType.FOOTER, source, columns, exporter, context);
	}
	
	private List<List<String>> exportFacet(FacetType facetType, HtmlDataTable source, List<UIColumn> columns, IExportType<?, ?, ?> exporter, FacesContext context) {
		List<List<String>> columnNames = new ArrayList<List<String>>();
		List<IExportCell> facetCells = new ArrayList<IExportCell>();
		
		boolean hasFacet = false;
		for (UIColumn column : columns) {
			String facetText = JsfUtil.getColumnFacetText(column, facetType, context);
			if (facetText != null) {
				hasFacet = true;
			}
			columnNames.add(Arrays.asList(facetText));
		}
		if (hasFacet) {
			for (List<String> columnName : columnNames) {
				facetCells.add(new ExportCellImpl(FACET_NAMES.get(facetType), columnName.get(0), 1, 1));
			}
			exporter.exportRow(new ExportRowImpl(FACET_NAMES.get(facetType), null, facetType, facetCells));
		}
		
		for (int i = 0; i < columnNames.size(); i++) {
			columnNames.set(i, new ArrayList<String>(new LinkedHashSet<String>(columnNames.get(i))));
		}
		
		return columnNames;
	}
	
	private void exportRowCells(HtmlDataTable source, List<UIColumn> columns, List<List<String>> columnNames, int startingRow, int endingRow, IExportType<?, ?, ?> exporter, FacesContext context) {
		List<String> rowName = Arrays.asList(source.getVar());
		List<IExportCell> cells = new ArrayList<IExportCell>();
		int columnCount = columns.size();
		
		for (int i = startingRow; i < endingRow; i++) {
			source.setRowIndex(i);
			
			for (int j = 0; j < columnCount; j++) {
				UIColumn column = columns.get(j);
				
				cells.add(new ExportCellImpl(columnNames.get(j), ExportUtil.transformComponentsToString(context, column.getChildren()), 1, 1));
			}
			
			exporter.exportRow(new ExportRowImpl(rowName, null, null, cells));
			cells.clear();
		}
	}

}
