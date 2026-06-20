package ru.paulevs.bismuthlib.gui;

import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CFSettingsScreen extends OptionsSubScreen {
	private OptionsList list;

	public CFSettingsScreen(Screen screen, Options options) {
		super(screen, options, Component.translatable("bismuthlib.options.settings.title"));
	}

	@Override
	protected void init() {
		this.list = new OptionsList(this.minecraft, this.width, this.height, this);
		this.list.addBig(CFOptions.MAP_RADIUS_XZ);
		this.list.addBig(CFOptions.MAP_RADIUS_Y);
		this.list.addBig(CFOptions.BRIGHTNESS);
		this.list.addSmall(CFOptions.OPTIONS);
		this.addRenderableWidget(this.list);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			CFOptions.save();
			ru.paulevs.bismuthlib.BismuthLibClient.initData();
			this.minecraft.setScreen(this.lastScreen);
		}).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFF);
	}
}