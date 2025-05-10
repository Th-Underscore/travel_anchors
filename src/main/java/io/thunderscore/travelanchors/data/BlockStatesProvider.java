package io.thunderscore.travelanchors.data;

import io.thunderscore.travelanchors.ModBlocks;
import org.moddingx.libx.datagen.DatagenContext;
import org.moddingx.libx.datagen.provider.model.BlockStateProviderBase;

public class BlockStatesProvider extends BlockStateProviderBase {

    public BlockStatesProvider(DatagenContext ctx) {
        super(ctx);
    }

    @Override
    protected void setup() {
        this.cubeAll(ModBlocks.travelAnchor);
    }
}
