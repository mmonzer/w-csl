package com.csl.modules;

import com.csl.core.CSLContext;
import com.csl.core.ModuleContext;
import com.csl.monitor.ActivityMonitor;
import com.ucsl.interfaces.IResult;
import lombok.Getter;
import lombok.Setter;

public class ModuleIDS {
    @Getter
    ActivityMonitor activityMonitor = new ActivityMonitor();

    @Setter
    boolean sendToBrowser = true;
    @Setter
    boolean sendToConsole = true;

    public IResult init(CSLContext context, ModuleContext mcontext) {
        return IResult.OK;
    }

    public IResult start(CSLContext context, ModuleContext mcontext) {
        // TODO Auto-generated method stub
        //context.getGlobalVariablesTable().createDoubleVariable("u",0);


        return IResult.OK;
    }

    public IResult stop(CSLContext context, ModuleContext mcontext) {
        // TODO Auto-generated method stub
        return IResult.OK;
    }

    static {
        CSLContext.instance.registerModuleClass("ModuleIDS", ModuleIDS.class);
    }
}
