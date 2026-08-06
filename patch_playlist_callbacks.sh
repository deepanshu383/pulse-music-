sed -i 's/sortOrder = order/sortOrder = order\n                                        onSortOrderChange(order)/' app/src/main/java/com/example/ui/PlaylistDetailsScreen.kt
sed -i 's/viewMode = PlaylistViewMode.LIST/viewMode = PlaylistViewMode.LIST; onViewModeChange(PlaylistViewMode.LIST)/' app/src/main/java/com/example/ui/PlaylistDetailsScreen.kt
sed -i 's/viewMode = PlaylistViewMode.GRID/viewMode = PlaylistViewMode.GRID; onViewModeChange(PlaylistViewMode.GRID)/' app/src/main/java/com/example/ui/PlaylistDetailsScreen.kt
sed -i 's/viewMode = PlaylistViewMode.CARD/viewMode = PlaylistViewMode.CARD; onViewModeChange(PlaylistViewMode.CARD)/' app/src/main/java/com/example/ui/PlaylistDetailsScreen.kt
