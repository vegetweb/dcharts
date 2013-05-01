/**
 * Copyright (C) 2012-2013  Dušan Vejnovič  <vaadin@dussan.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dussan.vaadin.dcharts;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.dussan.vaadin.dcharts.client.rpc.DChartsServerRpc;
import org.dussan.vaadin.dcharts.client.state.DChartsState;
import org.dussan.vaadin.dcharts.data.DataSeries;
import org.dussan.vaadin.dcharts.events.ChartData;
import org.dussan.vaadin.dcharts.events.chartImageChange.ChartImageChangeEvent;
import org.dussan.vaadin.dcharts.events.chartImageChange.ChartImageChangeHandler;
import org.dussan.vaadin.dcharts.events.click.ChartDataClickEvent;
import org.dussan.vaadin.dcharts.events.click.ChartDataClickHandler;
import org.dussan.vaadin.dcharts.events.mouseenter.ChartDataMouseEnterEvent;
import org.dussan.vaadin.dcharts.events.mouseenter.ChartDataMouseEnterHandler;
import org.dussan.vaadin.dcharts.events.mouseleave.ChartDataMouseLeaveEvent;
import org.dussan.vaadin.dcharts.events.mouseleave.ChartDataMouseLeaveHandler;
import org.dussan.vaadin.dcharts.events.rightclick.ChartDataRightClickEvent;
import org.dussan.vaadin.dcharts.events.rightclick.ChartDataRightClickHandler;
import org.dussan.vaadin.dcharts.helpers.ChartDataHelper;
import org.dussan.vaadin.dcharts.helpers.ManifestHelper;
import org.dussan.vaadin.dcharts.options.Options;

import com.google.gwt.event.shared.HandlerManager;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.AbstractSingleComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

public class DCharts extends AbstractSingleComponentContainer {

	private static final long serialVersionUID = -7224003274781707144L;
	private static final int ID = 0;
	private static final int DECIMAL_SEPARATOR = 1;
	private static final int THOUSANDS_SEPARATOR = 2;
	private static final int DATA_SERIES = 3;
	private static final int OPTIONS = 4;
	private static final int SHOW_CHART = 5;
	private static final int REPLOT_CHART_CLEAR = 6;
	private static final int REPLOT_CHART_RESET_AXES = 7;
	private static final int REFRESH_CHART = 8;
	private static final int MARGIN_TOP = 9;
	private static final int MARGIN_RIGHT = 10;
	private static final int MARGIN_BOTTOM = 11;
	private static final int MARGIN_LEFT = 12;
	private static final int MOUSE_ENTER_EVENT = 13;
	private static final int MOUSE_LEAVE_EVENT = 14;
	private static final int CLICK_EVENT = 15;
	private static final int RIGHT_CLICK_EVENT = 16;
	private static final int CHART_IMAGE_CHANGE_EVENT = 17;
	private static final int DOWNLOAD_BUTTON_ENABLE = 18;
	private static final int DOWNLOAD_BUTTON_LOCATION = 19;

	private byte[] chartImage = null;
	private Map<Integer, String> chartData = null;
	private ChartImageFormat chartImageFormat = null;

	private HandlerManager handlerManager = null;
	private DataSeries dataSeries = null;
	private Options options = null;

	private Boolean downloadButtonEnable = null;
	private Button downloadButton = null;
	private String downloadFilename = null;
	private DownloadButtonLocation downloadButtonLocation = null;
	private FileDownloader fileDownloader = null;

	private String decimalSeparator = null;
	private String thousandsSeparator = null;
	private Integer marginTop = null;
	private Integer marginRight = null;
	private Integer marginBottom = null;
	private Integer marginLeft = null;

	private Boolean enableChartDataMouseEnterEvent = null;
	private Boolean enableChartDataMouseLeaveEvent = null;
	private Boolean enableChartDataClickEvent = null;
	private Boolean enableChartDataRightClickEvent = null;
	private Boolean enableChartImageChangeEvent = null;

	public DCharts() {
		marginTop = 0;
		marginRight = 0;
		marginBottom = 0;
		marginLeft = 0;

		handlerManager = new HandlerManager(this);
		enableChartDataMouseEnterEvent = false;
		enableChartDataMouseLeaveEvent = false;
		enableChartDataClickEvent = false;
		enableChartDataRightClickEvent = false;
		enableChartImageChangeEvent = false;

		chartData = new HashMap<Integer, String>();
		chartImageFormat = ChartImageFormat.PNG;

		downloadButtonEnable = false;
		downloadFilename = "chart";
		downloadButtonLocation = DownloadButtonLocation.TOP_RIGHT;

		addChartContainer();
		registerRpc(new DChartsServerRpc() {
			private static final long serialVersionUID = -3805014254043430235L;

			@Override
			public void onEventFired(Map<String, String> eventData) {
				processEvent(eventData);
			}
		});
	}

	public DCharts(String caption, String idChart, DataSeries dataSeries,
			Options options) {
		this();
		setCaption(caption);
		this.dataSeries = dataSeries;
		this.options = options;
	}

	public DCharts(String caption, DataSeries dataSeries, Options options) {
		this();
		setCaption(caption);
		this.dataSeries = dataSeries;
		this.options = options;
	}

	public DCharts(DataSeries dataSeries, Options options) {
		this();
		this.dataSeries = dataSeries;
		this.options = options;
	}

	public DCharts(String caption, DataSeries dataSeries) {
		this();
		setCaption(caption);
		this.dataSeries = dataSeries;
	}

	public DCharts(DataSeries dataSeries) {
		this();
		this.dataSeries = dataSeries;
	}

	private StreamResource getChartResource() {
		return new StreamResource(new StreamSource() {
			private static final long serialVersionUID = -6463786579404065303L;

			@Override
			public InputStream getStream() {
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ImageIO.write(getChartImage(), getChartImageFormat()
							.getFormat(), bos);
					return new ByteArrayInputStream(bos.toByteArray());
				} catch (Exception e) {
					return new ByteArrayInputStream("".getBytes());
				}
			}
		}, getDownloadFilename() + getChartImageFormat().getFormatExtension());
	}

	private void addChartContainer() {
		String id = "dCharts-" + ((long) (Math.random() * 10000000000000000L));
		chartData.put(ID, id);

		downloadButton = new Button("Download chart");
		downloadButton.setId(id + "-button");
		downloadButton.addStyleName("small");
		downloadButton.setSizeUndefined();
		setContent(downloadButton);

		fileDownloader = new FileDownloader(getChartResource());
		fileDownloader.extend(downloadButton);

		setSizeFull();
		markAsDirty();
	}

	private void processEvent(Map<String, String> eventData) {
		if (eventData != null && !eventData.isEmpty()) {
			ChartData chartData = ChartDataHelper.process(eventData);
			if (chartData != null && chartData.getSeriesIndex() != null
					&& chartData.getPointIndex() != null) {
				chartData.setOriginData(dataSeries.getSeriesValue(chartData
						.getSeriesIndex().intValue(), chartData.getPointIndex()
						.intValue()));
			}

			if (chartData != null) {
				switch (chartData.getChartEventType()) {
				case BAR_MOUSE_ENTER:
				case BUBBLE_MOUSE_ENTER:
				case DONUT_MOUSE_ENTER:
				case OHLC_MOUSE_ENTER:
				case PIE_MOUSE_ENTER:
				case PYRAMID_MOUSE_ENTER:
					handlerManager.fireEvent(new ChartDataMouseEnterEvent(
							chartData));
					break;

				case BAR_MOUSE_LEAVE:
				case BUBBLE_MOUSE_LEAVE:
				case DONUT_MOUSE_LEAVE:
				case OHLC_MOUSE_LEAVE:
				case PIE_MOUSE_LEAVE:
				case PYRAMID_MOUSE_LEAVE:
					handlerManager.fireEvent(new ChartDataMouseLeaveEvent(
							chartData));
					break;

				case BAR_CLICK:
				case BUBBLE_CLICK:
				case DONUT_CLICK:
				case LINE_CLICK:
				case OHLC_CLICK:
				case PIE_CLICK:
					handlerManager
							.fireEvent(new ChartDataClickEvent(chartData));
					break;

				case BAR_RIGHT_CLICK:
				case BUBBLE_RIGHT_CLICK:
				case DONUT_RIGHT_CLICK:
				case LINE_RIGHT_CLICK:
				case PIE_RIGHT_CLICK:
					handlerManager.fireEvent(new ChartDataRightClickEvent(
							chartData));
					break;

				case RAW_IMAGE_DATA:
					try {
						String data = chartData.getData()[0].toString()
								.substring("data:image/png;base64,".length());
						chartImage = Base64.decodeBase64(data);
						downloadButton.setEnabled(chartImage.length > 0);
						fileDownloader
								.setFileDownloadResource(getChartResource());
						if (enableChartImageChangeEvent) {
							handlerManager.fireEvent(new ChartImageChangeEvent(
									getChartImage()));
						}
					} catch (Exception e) {
						downloadButton.setEnabled(false);
						if (enableChartImageChangeEvent) {
							handlerManager.fireEvent(new ChartImageChangeEvent(
									null));
						}
					}
					break;

				case NOT_DEFINED:
				default:
					String caption = "UNKNOWN EVENT";
					String description = "Cannot process unknown chart data event!";
					Notification.show(caption, description, Type.ERROR_MESSAGE);
					break;
				}
			}
		}
	}

	@Override
	protected DChartsState getState() {
		return (DChartsState) super.getState();
	}

	@Override
	public void beforeClientResponse(boolean initial) {
		super.beforeClientResponse(initial);
		if (chartData != null && chartData.size() > 0) {
			Map<Integer, String> cloneData = new HashMap<Integer, String>();
			cloneData.putAll(chartData);
			getState().setChartData(cloneData);
			chartData.clear();
		}
	}

	public static String getVersion() {
		if (ManifestHelper.getManifest() != null) {
			return ManifestHelper.getManifest().getMainAttributes()
					.getValue("Implementation-Version");
		}
		return null;
	}

	public static String getGitVersion() {
		if (ManifestHelper.getManifest() != null) {
			return ManifestHelper.getManifest().getMainAttributes()
					.getValue("Git-Version");
		}
		return null;
	}

	public static String getJqPlotVersion() {
		if (ManifestHelper.getManifest() != null) {
			return ManifestHelper.getManifest().getMainAttributes()
					.getValue("JqPlot-Version");
		}
		return null;
	}

	public BufferedImage getChartImage() {
		if (chartImage != null && chartImage.length > 0) {
			try {
				return ImageIO.read(new ByteArrayInputStream(chartImage));
			} catch (IOException e) {
				// not catch any error
			}
		}
		return null;
	}

	public ChartImageFormat getChartImageFormat() {
		return chartImageFormat;
	}

	public void setChartImageFormat(ChartImageFormat chartImageFormat) {
		this.chartImageFormat = chartImageFormat;
	}

	public void autoSelectDecimalAndThousandsSeparator(Locale locale) {
		decimalSeparator = Character.toString(((DecimalFormat) NumberFormat
				.getNumberInstance(locale)).getDecimalFormatSymbols()
				.getDecimalSeparator());
		thousandsSeparator = Character.toString(((DecimalFormat) NumberFormat
				.getNumberInstance(locale)).getDecimalFormatSymbols()
				.getGroupingSeparator());
		chartData.put(DECIMAL_SEPARATOR, decimalSeparator);
		chartData.put(THOUSANDS_SEPARATOR, thousandsSeparator);
	}

	public void autoSelectDecimalSeparator(Locale locale) {
		decimalSeparator = Character.toString(((DecimalFormat) NumberFormat
				.getNumberInstance(locale)).getDecimalFormatSymbols()
				.getDecimalSeparator());
		chartData.put(DECIMAL_SEPARATOR, decimalSeparator);
	}

	public void autoSelectThousandsSeparator(Locale locale) {
		thousandsSeparator = Character.toString(((DecimalFormat) NumberFormat
				.getNumberInstance(locale)).getDecimalFormatSymbols()
				.getGroupingSeparator());
		chartData.put(THOUSANDS_SEPARATOR, thousandsSeparator);
	}

	public String getDecimalSeparator() {
		return decimalSeparator;
	}

	public void setDecimalSeparator(String decimalSeparator) {
		if (decimalSeparator != null && decimalSeparator.length() > 0) {
			this.decimalSeparator = decimalSeparator;
			chartData.put(DECIMAL_SEPARATOR, decimalSeparator);
		}
	}

	public String getThousandsSeparator() {
		return thousandsSeparator;
	}

	public void setThousandsSeparator(String thousandsSeparator) {
		this.thousandsSeparator = thousandsSeparator;
		if (thousandsSeparator != null && thousandsSeparator.length() > 0) {
			this.thousandsSeparator = thousandsSeparator;
			chartData.put(THOUSANDS_SEPARATOR, thousandsSeparator);
		}

	}

	public DCharts setMargins(int marginTop, int marginRight, int marginBottom,
			int marginLeft) {
		this.marginTop = marginTop;
		this.marginRight = marginRight;
		this.marginBottom = marginBottom;
		this.marginLeft = marginLeft;
		chartData.put(MARGIN_TOP, Integer.toString(marginTop));
		chartData.put(MARGIN_RIGHT, Integer.toString(marginRight));
		chartData.put(MARGIN_BOTTOM, Integer.toString(marginBottom));
		chartData.put(MARGIN_LEFT, Integer.toString(marginLeft));
		return this;
	}

	public int getMarginTop() {
		return marginTop;
	}

	public DCharts setMarginTop(int marginTop) {
		this.marginTop = marginTop;
		chartData.put(MARGIN_TOP, Integer.toString(marginTop));
		return this;
	}

	public int getMarginRight() {
		return marginRight;
	}

	public DCharts setMarginRight(int marginRight) {
		this.marginRight = marginRight;
		chartData.put(MARGIN_RIGHT, Integer.toString(marginRight));
		return this;
	}

	public int getMarginBottom() {
		return marginBottom;
	}

	public DCharts setMarginBottom(int marginBottom) {
		this.marginBottom = marginBottom;
		chartData.put(MARGIN_BOTTOM, Integer.toString(marginBottom));
		return this;
	}

	public int getMarginLeft() {
		return marginLeft;
	}

	public DCharts setMarginLeft(int marginLeft) {
		this.marginLeft = marginLeft;
		chartData.put(MARGIN_LEFT, Integer.toString(marginLeft));
		return this;
	}

	public Boolean isEnableDownload() {
		return downloadButtonEnable;
	}

	public void setEnableDownload(boolean enable) {
		downloadButtonEnable = enable;
		chartData.put(DOWNLOAD_BUTTON_ENABLE, Boolean.toString(enable));
	}

	public String getDownloadFilename() {
		return downloadFilename;
	}

	public void setDownloadFilename(String filename) {
		if (filename != null && !filename.trim().isEmpty()) {
			downloadFilename = filename;
		}
	}

	public DownloadButtonLocation getDownloadButtonLocation() {
		return downloadButtonLocation;
	}

	public void setDownloadButtonLocation(DownloadButtonLocation location) {
		downloadButtonLocation = location;
		chartData.put(DOWNLOAD_BUTTON_LOCATION, location.toString());
	}

	public String getDownloadButtonCaption() {
		return downloadButton.getCaption();
	}

	public void setDownloadButtonCaption(String caption) {
		if (caption != null && !caption.trim().isEmpty()) {
			downloadButton.setCaption(caption);
		}
	}

	public DataSeries getDataSeries() {
		return dataSeries;
	}

	public DCharts setDataSeries(DataSeries dataSeries) {
		if (dataSeries != null && !dataSeries.isEmpty()) {
			this.dataSeries = dataSeries;
			chartData.put(DATA_SERIES, dataSeries.getValue());
		}
		return this;
	}

	public Options getOptions() {
		return options;
	}

	public DCharts setOptions(Options options) {
		if (options != null && !options.isEmpty()) {
			this.options = options;
			chartData.put(OPTIONS, options.getValue());
		}
		return this;
	}

	public boolean isEnableChartDataMouseEnterEvent() {
		return enableChartDataMouseEnterEvent;
	}

	public DCharts setEnableChartDataMouseEnterEvent(
			boolean enableChartDataMouseEnterEvent) {
		this.enableChartDataMouseEnterEvent = enableChartDataMouseEnterEvent;
		chartData.put(MOUSE_ENTER_EVENT,
				Boolean.toString(enableChartDataMouseEnterEvent));
		return this;
	}

	public boolean isEnableChartDataMouseLeaveEvent() {
		return enableChartDataMouseLeaveEvent;
	}

	public DCharts setEnableChartDataMouseLeaveEvent(
			boolean enableChartDataMouseLeaveEvent) {
		this.enableChartDataMouseLeaveEvent = enableChartDataMouseLeaveEvent;
		chartData.put(MOUSE_LEAVE_EVENT,
				Boolean.toString(enableChartDataMouseLeaveEvent));
		return this;
	}

	public boolean isEnableChartDataClickEvent() {
		return enableChartDataClickEvent;
	}

	public DCharts setEnableChartDataClickEvent(
			boolean enableChartDataClickEvent) {
		this.enableChartDataClickEvent = enableChartDataClickEvent;
		chartData.put(CLICK_EVENT, Boolean.toString(enableChartDataClickEvent));
		return this;
	}

	public boolean isEnableChartDataRightClickEvent() {
		return enableChartDataRightClickEvent;
	}

	public DCharts setEnableChartDataRightClickEvent(
			boolean enableChartDataRightClickEvent) {
		this.enableChartDataRightClickEvent = enableChartDataRightClickEvent;
		chartData.put(RIGHT_CLICK_EVENT,
				Boolean.toString(enableChartDataRightClickEvent));
		return this;
	}

	public boolean isEnableChartImageChangeEvent() {
		return enableChartImageChangeEvent;
	}

	public DCharts setEnableChartImageChangeEvent(
			boolean enableChartImageChangeEvent) {
		this.enableChartImageChangeEvent = enableChartImageChangeEvent;
		chartData.put(CHART_IMAGE_CHANGE_EVENT,
				Boolean.toString(enableChartImageChangeEvent));
		return this;
	}

	public DCharts show() {
		if (dataSeries != null && !dataSeries.isEmpty()) {
			chartData.put(SHOW_CHART, Boolean.TRUE.toString());
			markAsDirty();
		}
		return this;
	}

	public DCharts hide() {
		chartData.put(SHOW_CHART, Boolean.FALSE.toString());
		markAsDirty();
		return this;
	}

	public DCharts replot(boolean clean, boolean resetAxes) {
		chartData.put(REPLOT_CHART_CLEAR, Boolean.toString(resetAxes));
		chartData.put(REPLOT_CHART_RESET_AXES, Boolean.toString(resetAxes));
		markAsDirty();
		return this;
	}

	public DCharts refresh() {
		chartData.put(REFRESH_CHART, Boolean.TRUE.toString());
		markAsDirty();
		return this;
	}

	public void addHandler(ChartDataMouseEnterHandler handler) {
		if (isEnableChartDataMouseEnterEvent()) {
			handlerManager.addHandler(ChartDataMouseEnterEvent.getType(),
					handler);
		}
	}

	public void removeHandler(ChartDataMouseEnterHandler handler) {
		if (handlerManager.isEventHandled(ChartDataMouseEnterEvent.getType())) {
			handlerManager.removeHandler(ChartDataMouseEnterEvent.getType(),
					handler);
		}
	}

	public void addHandler(ChartDataMouseLeaveHandler handler) {
		if (isEnableChartDataMouseLeaveEvent()) {
			handlerManager.addHandler(ChartDataMouseLeaveEvent.getType(),
					handler);
		}
	}

	public void removeHandler(ChartDataMouseLeaveHandler handler) {
		if (handlerManager.isEventHandled(ChartDataMouseLeaveEvent.getType())) {
			handlerManager.removeHandler(ChartDataMouseLeaveEvent.getType(),
					handler);
		}
	}

	public void addHandler(ChartDataClickHandler handler) {
		if (isEnableChartDataClickEvent()) {
			handlerManager.addHandler(ChartDataClickEvent.getType(), handler);
		}
	}

	public void removeHandler(ChartDataClickHandler handler) {
		if (handlerManager.isEventHandled(ChartDataClickEvent.getType())) {
			handlerManager
					.removeHandler(ChartDataClickEvent.getType(), handler);
		}
	}

	public void addHandler(ChartDataRightClickHandler handler) {
		if (isEnableChartDataRightClickEvent()) {
			handlerManager.addHandler(ChartDataRightClickEvent.getType(),
					handler);
		}
	}

	public void removeHandler(ChartDataRightClickHandler handler) {
		if (handlerManager.isEventHandled(ChartDataRightClickEvent.getType())) {
			handlerManager.removeHandler(ChartDataRightClickEvent.getType(),
					handler);
		}
	}

	public void addHandler(ChartImageChangeHandler handler) {
		handlerManager.addHandler(ChartImageChangeEvent.getType(), handler);
	}

	public void removeHandler(ChartImageChangeHandler handler) {
		if (handlerManager.isEventHandled(ChartImageChangeEvent.getType())) {
			handlerManager.removeHandler(ChartImageChangeEvent.getType(),
					handler);
		}
	}

}
