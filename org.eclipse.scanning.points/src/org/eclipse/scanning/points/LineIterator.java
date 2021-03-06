/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.points;

import java.util.Arrays;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.CollatedStepModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

class LineIterator extends AbstractScanPointIterator {

	private StepModel model;
	private double value;
	private int index;

	public LineIterator(StepGenerator gen) {
		this.model = gen.getModel();
		value = model.getStart() - model.getStep();

        JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

        String name   = model.getName();
        double start  = model.getStart();
        double stop   = model.getStop();
        int numPoints = model.size();

		ScanPointIterator iterator = lineGeneratorFactory.createObject(name, "mm", start, stop, numPoints);
		pyIterator = iterator;
		this.index = 0;
	}

	public LineIterator(OneDEqualSpacingGenerator gen) {
		OneDEqualSpacingModel model= gen.getModel();
		BoundingLine line = model.getBoundingLine();

		JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		int numPoints = model.getPoints();
		double step = line.getLength() / numPoints;
		double xStep = step * Math.cos(line.getAngle());
		double yStep = step * Math.sin(line.getAngle());

		PyList names =  new PyList(Arrays.asList(new String[] {model.getFastAxisName(), model.getSlowAxisName()}));
		PyList units = new PyList(Arrays.asList(new String[] {"mm", "mm"}));
		double[] start = {line.getxStart() + xStep/2, line.getyStart() + yStep/2};
		double[] stop = {line.getxStart() + xStep * (numPoints - 0.5), line.getyStart() + yStep * (numPoints - 0.5)};

		ScanPointIterator iterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		pyIterator = iterator;
		this.index = 0;
	}

	public LineIterator(OneDStepGenerator gen) {
		OneDStepModel model= gen.getModel();
		BoundingLine line = model.getBoundingLine();

        JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		int numPoints = (int) Math.floor(line.getLength() / model.getStep()) + 1;
        double xStep = model.getStep() * Math.cos(line.getAngle());
        double yStep = model.getStep() * Math.sin(line.getAngle());

		PyList names =  new PyList(Arrays.asList(new String[] {model.getFastAxisName(), model.getSlowAxisName()}));
		PyList units = new PyList(Arrays.asList(new String[] {"mm", "mm"}));
		double[] start = {line.getxStart(), line.getyStart()};
        double[] stop = {line.getxStart() + xStep * numPoints, line.getyStart() + yStep * numPoints};

		ScanPointIterator iterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		pyIterator = iterator;
		this.index = 0;
	}

	@Override
	public boolean hasNext() {
		return pyIterator.hasNext();
	}

	@Override
	public IPosition next() {

		IPosition next = null;
        if (model instanceof CollatedStepModel) { // For AnnotatedScanTest
			@SuppressWarnings("unchecked")
			Scalar<Double> point = (Scalar<Double>) pyIterator.next();
			value = point.getValue();
		final MapPosition mp = new MapPosition();
		for (String name : ((CollatedStepModel)model).getNames()) {
			mp.put(name, value);
			mp.putIndex(name, -1);
			}
		next = mp;

        } else {
		next = pyIterator.next();
        }
        if (next!=null && model!=null) {
	        next.setExposureTime(model.getExposureTime()); // Usually 0
	        next.setStepIndex(index);
        }
        ++index;
        return next;
	}

	@Override
	public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
