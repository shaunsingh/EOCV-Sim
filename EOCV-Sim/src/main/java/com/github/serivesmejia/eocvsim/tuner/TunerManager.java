/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.tuner;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.tuner.scanner.AnnotatedTunableFieldScanner;
import com.github.serivesmejia.eocvsim.util.Log;
import com.github.serivesmejia.eocvsim.util.ReflectUtil;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("rawtypes")
public class TunerManager {

    private final EOCVSim eocvSim;

    private final List<TunableField> fields = new ArrayList<>();

    private TunableFieldAcceptorManager acceptorManager = null;

    private static HashMap<Type, Class<? extends TunableField<?>>> tunableFieldsTypes = null;
    private static HashMap<Class<? extends TunableField<?>>, Class<? extends TunableFieldAcceptor>> tunableFieldAcceptors = null;

    private boolean firstInit = true;

    public TunerManager(EOCVSim eocvSim) {
        this.eocvSim = eocvSim;
    }

    public void init() {
        if(tunableFieldsTypes == null) {
            AnnotatedTunableFieldScanner.ScanResult result = new AnnotatedTunableFieldScanner(
                    eocvSim.getParams().getScanForTunableFieldsIn()
            ).scan();

            tunableFieldsTypes = result.getTunableFields();
            tunableFieldAcceptors = result.getAcceptors();
        }

        // for some reason, acceptorManager becomes null after a certain time passes
        // (maybe garbage collected? i don't know for sure...), but we can simply recover
        // from this by creating a new one with the found acceptors by the scanner, no problem.
        if(acceptorManager == null)
            acceptorManager = new TunableFieldAcceptorManager(tunableFieldAcceptors);

        if (firstInit) {
            eocvSim.pipelineManager.onPipelineChange.doPersistent(this::reset);
            firstInit = false;
        }

        if (eocvSim.pipelineManager.getCurrentPipeline() != null) {
            addFieldsFrom(eocvSim.pipelineManager.getCurrentPipeline());
            eocvSim.visualizer.updateTunerFields(createTunableFieldPanels());

            for(TunableField field : fields) {
                field.init();
            }
        }
    }

    public void update() {
        //update all fields
        for(TunableField field : fields.toArray(new TunableField[0])) {
            try {
                field.update();
            } catch(Exception ex) {
                Log.error("Error while updating field " + field.getFieldName(), ex);
            }

            //check if this field has requested to reevaluate config for all panels
            if(field.fieldPanel.hasRequestedAllConfigReeval()) {
                //if so, iterate through all fields to reevaluate
                for(TunableField f : fields.toArray(new TunableField[0])) {
                    f.fieldPanel.panelOptions.reevaluateConfig();
                }
            }
        }
    }

    public void reset() {
        fields.clear();
        init();
    }

    public void addFieldsFrom(OpenCvPipeline pipeline) {

        if (pipeline == null) return;

        Field[] fields = pipeline.getClass().getFields();

        for (Field field : fields) {

            //we only accept non-final fields
            if (Modifier.isFinal(field.getModifiers())) continue;

            Class<?> type = field.getType();
            if (field.getType().isPrimitive()) { //wrap to java object equivalent if field type is primitive
                type = ReflectUtil.wrap(type);
            }

            Class<? extends TunableField> tunableFieldClass = null;

            if(tunableFieldsTypes.containsKey(type)) {
                tunableFieldClass = tunableFieldsTypes.get(type);
            } else {
                //if we don't have a class yet, use our acceptors
                if(acceptorManager != null) tunableFieldClass = acceptorManager.accept(type);
                //still haven't got anything, give up here.
                if(tunableFieldClass == null) continue;
            }

            //yay we have a registered TunableField which handles this
            //now, lets do some more reflection to instantiate this TunableField
            //and add it to the list...
            try {
                Constructor<? extends TunableField> constructor = tunableFieldClass.getConstructor(OpenCvPipeline.class, Field.class, EOCVSim.class);
                this.fields.add(constructor.newInstance(pipeline, field, eocvSim));
            } catch (Exception ex) {
                //oops rip
                Log.error("TunerManager", "Reflection error while processing field: " + field.getName(), ex);
            }

        }
    }

    public void reevaluateConfigs() {
        for(TunableField field : fields) {
            field.fieldPanel.panelOptions.reevaluateConfig();
        }
    }

    private List<TunableFieldPanel> createTunableFieldPanels() {
        List<TunableFieldPanel> panels = new ArrayList<>();

        for (TunableField field : fields) {
            panels.add(new TunableFieldPanel(field, eocvSim));
        }

        return panels;
    }

}
