INSERT INTO venues (name, neighborhood, vibe, music, symbol_name, gradient_hex)
VALUES
  ('Pulse Room', 'SoMa', 'High-energy dance floor', 'House / EDM', 'waveform.path.ecg', ARRAY['7C3AED', 'EC4899']),
  ('Neon Garden', 'Mission', 'Loungey rooftop crowd', 'Afrobeat / Pop', 'sparkles', ARRAY['2563EB', '06B6D4']),
  ('Velvet Underground', 'North Beach', 'Intimate speakeasy', 'Disco / Funk', 'music.note.house.fill', ARRAY['0F766E', '22C55E'])
ON CONFLICT DO NOTHING;
