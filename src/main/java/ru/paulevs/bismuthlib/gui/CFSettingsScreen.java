package ru.paulevs.bismuthlib.gui;

import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CFSettingsScreen extends OptionsSubScreen {
	public CFSettingsScreen(Screen screen, Options options) {
		super(screen, options, Component.translatable("bismuthlib.options.settings.title"));
	}
	
	@Override
	protected void addOptions() {
		this.list.addBig(CFOptions.MAP_RADIUS_XZ);
		this.list.addBig(CFOptions.MAP_RADIUS_Y);
		this.list.addBig(CFOptions.BRIGHTNESS);
		this.list.addSmall(CFOptions.OPTIONS);
	}
	
	@Override
	protected void addFooter() {
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> {
			CFOptions.save();
			ru.paulevs.bismuthlib.BismuthLibClient.initData();
			this.minecraft.setScreen(this.lastScreen);
		}).width(200).build());
	}
}
