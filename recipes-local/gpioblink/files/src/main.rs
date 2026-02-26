use gpio_cdev::{Chip, LineRequestFlags};
use std::error::Error;
use std::thread;
use std::time::Duration;

const CONSUMER: &str = "LED_Flasher";
const LED_PIN: u32 = 4; // adatta al tuo wiring
const CHIP: &str = "/dev/gpiochip1"; // adatta se serve

fn main() -> Result<(), Box<dyn Error>> {
    let mut chip = Chip::new(CHIP)?;                            // open
    let line = chip.get_line(LED_PIN)?;                         // get line
    let handle = line.request(LineRequestFlags::OUTPUT, 0, CONSUMER)?; // request as output low

    for _ in 0..5 {
        handle.set_value(1)?;                                   // LED on
        thread::sleep(Duration::from_secs(1));
        handle.set_value(0)?;                                   // LED off
        thread::sleep(Duration::from_secs(1));
    }

    // handle e chip si rilasciano da soli qui (Drop)
    Ok(())
}

