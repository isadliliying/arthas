package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RecorderModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

public class RecorderView extends ResultView<RecorderModel> {
    @Override
    public void draw(CommandProcess process, RecorderModel result) {
        process.write(result.getContent()).write("\n");
    }
}
